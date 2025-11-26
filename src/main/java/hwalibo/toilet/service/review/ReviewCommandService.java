package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.exception.review.ReviewNotFoundException;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.dto.review.request.ReviewUpdateRequest;
import hwalibo.toilet.service.user.UserRankService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;
    private final UserRankService userRankService;

    @Transactional
    public void deleteMyReview(User loginUser, Long reviewId) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("리뷰가 존재하지 않습니다."));

        if (!review.getUser().getId().equals(loginUser.getId())) {
            throw new SecurityException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        User author = review.getUser();
        Toilet toilet = review.getToilet();

        if (author != null) {
            author.removeReview();
        }
        if (toilet != null) {
            double star = review.getStar() != null ? review.getStar() : 0.0;
            toilet.removeReviewStats(star);
        }

        reviewRepository.delete(review);
        userRankService.evictUserRate(loginUser.getId());
    }

    @Transactional
    public Long updateMyReview(User loginUser, Long reviewId, ReviewUpdateRequest request) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("리뷰가 존재하지 않습니다."));
        if (!review.getUser().getId().equals(loginUser.getId())) {
            throw new SecurityException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        try {
            if (request.getStar() != null) {
                java.lang.reflect.Field f = Review.class.getDeclaredField("star");
                f.setAccessible(true);
                f.set(review, request.getStar());
            }
            if (request.getDescription() != null) {
                java.lang.reflect.Field f = Review.class.getDeclaredField("description");
                f.setAccessible(true);
                f.set(review, request.getDescription());
            }
            if (request.getTag() != null) {
                java.lang.reflect.Field f = Review.class.getDeclaredField("tag");
                f.setAccessible(true);
                f.set(review, request.getTag());
            }
            review.updateIsDis(request.isDis());
        } catch (Exception e) {
            throw new IllegalArgumentException("업데이트 실패: " + e.getMessage());
        }
        userRankService.evictUserRate(loginUser.getId());
        return review.getId();
    }
}

