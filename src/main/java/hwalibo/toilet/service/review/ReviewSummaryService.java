package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.dto.chat.request.ChatCompletionRequest;
import hwalibo.toilet.dto.chat.response.ChatCompletionResponse;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.response.ReviewSummaryResponse;
import hwalibo.toilet.respository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ReviewRepository reviewRepository;

    @Qualifier("openAIWebClient")
    private final WebClient openAIWebClient;

    @Value("${openai.chat-model:gpt-4o-mini}")
    private String model;

    // ReviewSummaryService.java (핵심 변경만)

    @Transactional(readOnly = true)
    public ApiResponse<ReviewSummaryResponse> summarizeByToiletId(Long toiletId) {
        var reviews = reviewRepository.findByToiletIdOrderByCreatedAtAsc(toiletId);
        if (reviews.isEmpty()) {
            return new ApiResponse<>(false, 404, "해당 화장실에 리뷰가 없습니다.");
        }

        String combined = reviews.stream()
                .map(Review::getDescription)
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");

        if (combined.isBlank()) {
            return new ApiResponse<>(false, 404, "리뷰 내용이 없습니다.");
        }

        try {
            // 1) 프롬프트를 짧게+완문 지시
            ChatCompletionRequest req = new ChatCompletionRequest();
            req.setModel(model);
            req.setMax_tokens(120);      // 여유 있게 제한
            req.setTemperature(0.3);

            String instruction = """
                아래 리뷰들을 180바이트 이내로 한국어로 1~2문장으로 요약하세요.
                반드시 완전한 문장으로 끝내고 마지막은 마침표로 끝내세요.
                불필요한 서론/결론/이모지/카테고리 없이 핵심만 적으세요.
                """;

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", instruction));
            messages.add(Map.of("role", "user", "content", combined));
            req.setMessages(messages);

            ChatCompletionResponse res = openAIWebClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(s -> s.value() == 401 || s.value() == 403,
                            r -> Mono.error(new RuntimeException("OpenAI 인증 실패(키 확인)")))
                    .onStatus(s -> s.value() == 429,
                            r -> Mono.error(new RuntimeException("요청 과다(429) - 잠시 후 재시도")))
                    .onStatus(s -> s.is5xxServerError(),
                            r -> Mono.error(new RuntimeException("OpenAI 서버 오류")))
                    .bodyToMono(ChatCompletionResponse.class)
                    .block();

            if (res == null || res.getChoices() == null || res.getChoices().isEmpty()
                    || res.getChoices().get(0).getMessage() == null) {
                return new ApiResponse<>(false, 500, "요약 응답이 비어 있습니다.");
            }

            String raw = res.getChoices().get(0).getMessage().getContent();
            String summary = formatToMaxBytes(raw, 200); // ★ 새 후처리 적용

            return new ApiResponse<>(true, 200, "리뷰 요약 성공",
                    new ReviewSummaryResponse(summary));

        } catch (Exception e) {
            log.error("❌ 리뷰 요약 중 오류", e);
            return new ApiResponse<>(false, 500, "리뷰 요약 생성 실패");
        }
    }

    /* ---------- Helpers: 바이트 기준 안전 컷 + 문장 경계 스냅 ---------- */
    private static final Set<Character> END_PUNCT = Set.of('.', '!', '?', '。', '！', '？', '…');

    private String formatToMaxBytes(String text, int maxBytes) {
        String s = (text == null ? "" : text).trim().replaceAll("\\s+", " ");

        // 이미 짧으면: 마무리만 보정
        if (utf8Len(s) <= maxBytes) return ensureSentenceClosed(s, maxBytes);

        // 1) maxBytes 안쪽의 '문장 끝 구두점' 직후로 자르기
        int endIdx = lastSentenceEndWithinBytes(s, maxBytes);
        if (endIdx >= 0) {
            return s.substring(0, endIdx + 1);
        }

        // 2) 문장 경계가 없다면: 말줄임(…)으로 명확히 표시
        String truncated = truncateUtf8(s, Math.max(0, maxBytes - 3));
        return truncated + "…";
    }

    private String ensureSentenceClosed(String s, int maxBytes) {
        if (!s.isEmpty() && END_PUNCT.contains(s.charAt(s.length() - 1))) return s;
        // 끝에 마침표 추가(바이트 초과 시 안전히 줄이고 추가)
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
        while (cut > 0 && (bytes[cut] & 0xC0) == 0x80) cut--; // 멀티바이트 경계 보정
        return new String(bytes, 0, cut, StandardCharsets.UTF_8);
    }

}