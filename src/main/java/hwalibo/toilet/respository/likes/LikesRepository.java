package hwalibo.toilet.respository.likes;
import hwalibo.toilet.domain.likes.Likes;
import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikesRepository extends JpaRepository<Likes, Long> {
    /**
     * 특정 사용자가 특정 리뷰에 '좋아요'를 눌렀는지 확인할 때 사용합니다.
     * '좋아요' 기능 구현의 핵심 메소드입니다.
     * @param user 사용자 엔티티
     * @param review 리뷰 엔티티
     * @return Optional<Likes>
     */
    Optional<Likes> findByUserAndReview(User user, Review review);
}