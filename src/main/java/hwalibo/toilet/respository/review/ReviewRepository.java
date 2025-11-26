package hwalibo.toilet.respository.review;

import hwalibo.toilet.domain.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByToiletIdOrderByCreatedAtAsc(Long toiletId);
}