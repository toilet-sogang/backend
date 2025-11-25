package hwalibo.toilet.service.review.summary;

import hwalibo.toilet.dto.chat.request.ChatCompletionRequest;
import hwalibo.toilet.dto.chat.response.ChatCompletionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component // 👈 서비스가 아닌 독립적인 부품이므로 @Component 사용
@RequiredArgsConstructor
public class OpenAISummaryProvider {

    private final WebClient openAIWebClient;

    @Value("${openai.chat-model:gpt-4o-mini}")
    private String model;

    @Cacheable(value = "review-summaries", key = "#combinedText.hashCode()")
    public String getSummaryFromOpenAI(String combinedText) {
        log.info("===== 🚽 OpenAI API 호출을 시작합니다... =====");

        String instruction = """
            아래 리뷰들을 180바이트 이내로 한국어로 1~2문장으로 요약하세요.
            반드시 완전한 문장으로 끝내고 마지막은 마침표로 끝내세요.
            불필요한 서론/결론/이모지/카테고리 없이 핵심만 적으세요.
            """;
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", instruction));
        messages.add(Map.of("role", "user", "content", combinedText));

        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setModel(model);
        req.setMax_tokens(120);
        req.setTemperature(0.3);
        req.setMessages(messages);

        ChatCompletionResponse res = openAIWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(s -> s.is4xxClientError(), r -> Mono.error(new RuntimeException("OpenAI API 요청 실패")))
                .onStatus(s -> s.is5xxServerError(), r -> Mono.error(new RuntimeException("OpenAI 서버 오류")))
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        if (res == null || res.getChoices() == null || res.getChoices().isEmpty()
                || res.getChoices().get(0).getMessage() == null) {
            throw new RuntimeException("요약 응답이 비어 있습니다.");
        }

        String raw = res.getChoices().get(0).getMessage().getContent();
        return formatToMaxBytes(raw, 200);
    }

    /* ---------- Helpers: 바이트 기준 안전 컷 + 문장 경계 스냅 ---------- */
    private static final Set<Character> END_PUNCT = Set.of('.', '!', '?', '。', '！', '？', '…');

    private String formatToMaxBytes(String text, int maxBytes) {
        String s = (text == null ? "" : text).trim().replaceAll("\\s+", " ");

        if (utf8Len(s) <= maxBytes) return ensureSentenceClosed(s, maxBytes);

        int endIdx = lastSentenceEndWithinBytes(s, maxBytes);
        if (endIdx >= 0) {
            return s.substring(0, endIdx + 1);
        }
        String truncated = truncateUtf8(s, Math.max(0, maxBytes - 3));
        return truncated + "…";
    }

    private String ensureSentenceClosed(String s, int maxBytes) {
        if (!s.isEmpty() && END_PUNCT.contains(s.charAt(s.length() - 1))) return s;
        if (utf8Len(s) + 1 <= maxBytes) return s + ".";
        return truncateUtf8(s, Math.max(0, maxBytes - 1)) + ".";
    }

    private int lastSentenceEndWithinBytes(String s, int maxBytes) {
        int bytes = 0;
        int lastEnd = -1;
        for (int i = 0; i < s.length(); i++) {
            int b = s.substring(i, i + 1).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + b > maxBytes) break;
            char ch = s.charAt(i);
            bytes += b;
            if (END_PUNCT.contains(ch)) lastEnd = i;
        }
        return lastEnd;
    }

    private int utf8Len(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }

    private String truncateUtf8(String text, int maxBytes) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return text;
        int cut = maxBytes;
        while (cut > 0 && (bytes[cut] & 0xC0) == 0x80) cut--;
        return new String(bytes, 0, cut, StandardCharsets.UTF_8);
    }
}