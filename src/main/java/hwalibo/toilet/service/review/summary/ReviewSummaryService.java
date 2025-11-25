package hwalibo.toilet.service.review.summary;
import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.dto.review.response.ReviewSummaryResponse;
import hwalibo.toilet.exception.review.ReviewNotFoundException;
import hwalibo.toilet.exception.review.SummaryGenerationException;
import hwalibo.toilet.respository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ReviewRepository reviewRepository;
    private final OpenAISummaryProvider openAISummaryProvider; // 👈 [수정] 새로운 전문가 주입

    @Transactional(readOnly = true)
    public ReviewSummaryResponse summarizeByToiletId(Long toiletId) {
        List<Review> reviews = reviewRepository.findByToiletIdOrderByCreatedAtAsc(toiletId);
        if (reviews.isEmpty()) {
            // 👈 [수정] 404 에러에 해당하는 예외를 던집니다.
            throw new ReviewNotFoundException("해당 화장실에 리뷰가 없습니다.");
        }

        String combined = reviews.stream()
                .map(Review::getDescription)
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");

        if (combined.isBlank()) {
            // 👈 [수정] 요약할 내용이 없는 경우에도 예외를 던집니다.
            throw new ReviewNotFoundException("요약할 리뷰 내용이 없습니다.");
        }

        try {
            // 전문가에게 요약을 요청하고, 성공 시 순수한 결과(String)를 받습니다.
            String summary = openAISummaryProvider.getSummaryFromOpenAI(combined);
            // 👈 [수정] 성공 시 순수한 데이터(DTO) 객체만 생성하여 반환합니다.
            return new ReviewSummaryResponse(summary);
        } catch (Exception e) {
            log.error("❌ 리뷰 요약 중 오류", e);
            // 👈 [수정] 500 에러에 해당하는 예외를 던집니다.
            throw new SummaryGenerationException("리뷰 요약 생성 중 오류가 발생했습니다.");
        }
    }
}