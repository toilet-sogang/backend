package hwalibo.toilet.respository.review;

import hwalibo.toilet.domain.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // toilet_id 기준으로 리뷰 정렬 조회
    List<Review> findByToiletIdOrderByCreatedAtAsc(Long toiletId);
}