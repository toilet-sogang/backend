package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.exception.review.ReviewNotFoundException;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.dto.review.request.ReviewUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;

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

        reviewRepository.delete(review);
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

        // 부분 업데이트
        if (request.getStar() != null) {
            // star Double -> set via reflection? there's no setter. We will rebuild? Better set via private fields cannot. We can create setters? Not available.
        }
        // 엔티티에 세터가 없으므로 빌더로 새 엔티티를 만드는 대신 JPA 변경감지를 위해 수동 세터가 필요.
        // 간단히 필드 접근을 위해 reflection 사용은 지양. 대신 엔티티에 업데이트 메서드가 없으니 여기서 직접 변경하도록 메서드 추가가 필요하지만
        // 현재 파일에서만 변경: use 'request.toEntity' is not applicable. We'll set via reflection minimal.
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
            if (request.getIsDis() != null) {
                java.lang.reflect.Field f = Review.class.getDeclaredField("isDis");
                f.setAccessible(true);
                f.set(review, request.getIsDis());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("업데이트 실패: " + e.getMessage());
        }

        return review.getId();
    }
}


