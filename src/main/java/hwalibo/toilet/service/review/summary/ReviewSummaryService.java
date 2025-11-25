package hwalibo.toilet.service.review.summary;
import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.dto.review.response.summary.ReviewSummaryResponse;
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
    private final OpenAISummaryProvider openAISummaryProvider;

    @Transactional(readOnly = true)
    public ReviewSummaryResponse summarizeByToiletId(Long toiletId) {
        List<Review> reviews = reviewRepository.findByToiletIdOrderByCreatedAtAsc(toiletId);
        if (reviews.isEmpty()) {
            throw new ReviewNotFoundException("해당 화장실에 리뷰가 없습니다.");
        }
        String combined = reviews.stream()
                .map(Review::getDescription)
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");
        if (combined.isBlank()) {
            throw new ReviewNotFoundException("요약할 리뷰 내용이 없습니다.");
        }
        try {
            String summary = openAISummaryProvider.getSummaryFromOpenAI(combined);
            return new ReviewSummaryResponse(summary);
        } catch (Exception e) {
            log.error("❌ 리뷰 요약 중 오류", e);
            throw new SummaryGenerationException("리뷰 요약 생성 중 오류가 발생했습니다.");
        }
    }
}