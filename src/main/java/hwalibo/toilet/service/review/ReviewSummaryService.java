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
            // 요청 DTO 채우기 (messages 직접 세팅)
            ChatCompletionRequest req = new ChatCompletionRequest();
            req.setModel(model);
            req.setMax_tokens(150);
            req.setTemperature(0.3);
            List<Map<String, String>> messages = new ArrayList<>();
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

            String summary = res.getChoices().get(0).getMessage().getContent();
            summary = truncateUtf8(summary == null ? "" : summary.trim(), 200);

            return new ApiResponse<>(true, 200, "리뷰 요약 성공",
                    new ReviewSummaryResponse(summary));

        } catch (Exception e) {
            log.error("❌ 리뷰 요약 중 오류", e);
            return new ApiResponse<>(false, 500, "리뷰 요약 생성 실패");
        }
    }

    private String truncateUtf8(String text, int maxBytes) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return text;
        int cut = maxBytes;
        while (cut > 0 && (bytes[cut] & 0xC0) == 0x80) cut--;
        return new String(bytes, 0, cut, StandardCharsets.UTF_8);
    }
}