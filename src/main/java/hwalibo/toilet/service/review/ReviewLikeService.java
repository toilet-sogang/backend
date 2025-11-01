package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.likes.Likes;
import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.exception.review.AlreadyLikedException;
import hwalibo.toilet.exception.review.ReviewNotFoundException;
import hwalibo.toilet.exception.review.NotLikedException;
import hwalibo.toilet.respository.likes.LikesRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewLikeService {

    private final ReviewRepository reviewRepository;
    private final LikesRepository likesRepository;

    @Transactional
    public void like(User loginUser, Long toiletId, Long reviewId) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("리뷰를 찾을 수 없습니다."));

        if (!review.getToilet().getId().equals(toiletId)) {
            throw new ReviewNotFoundException("리뷰를 찾을 수 없습니다.");
        }

        // 이미 좋아요 했는지 확인
        likesRepository.findByUserAndReview(loginUser, review)
                .ifPresent(l -> { throw new AlreadyLikedException("이미 좋아요를 누른 리뷰입니다."); });

        // 저장
        likesRepository.save(Likes.builder()
                .user(loginUser)
                .review(review)
                .build());

        // Review.good 증가 (엔티티에 세터가 없어 리플렉션 사용)
        try {
            java.lang.reflect.Field good = Review.class.getDeclaredField("good");
            good.setAccessible(true);
            Integer cur = (Integer) good.get(review);
            good.set(review, (cur == null ? 0 : cur) + 1);
        } catch (Exception ignored) {
            // 카운트 필드가 없거나 접근 실패 시 무시 (핵심 로직은 Likes 테이블 저장)
        }
    }

    @Transactional
    public void unlike(User loginUser, Long toiletId, Long reviewId) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("리뷰를 찾을 수 없습니다."));

        if (!review.getToilet().getId().equals(toiletId)) {
            throw new ReviewNotFoundException("리뷰를 찾을 수 없습니다." );
        }

        Likes like = likesRepository.findByUserAndReview(loginUser, review)
                .orElseThrow(() -> new NotLikedException("좋아요를 누르지 않은 리뷰입니다."));

        likesRepository.delete(like);

        try {
            java.lang.reflect.Field good = Review.class.getDeclaredField("good");
            good.setAccessible(true);
            Integer cur = (Integer) good.get(review);
            if (cur != null && cur > 0) {
                good.set(review, cur - 1);
            }
        } catch (Exception ignored) {}
    }
}