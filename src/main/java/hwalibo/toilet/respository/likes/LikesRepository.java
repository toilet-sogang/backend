package hwalibo.toilet.respository.likes;
import hwalibo.toilet.domain.likes.Likes;
import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikesRepository extends JpaRepository<Likes, Long> {
    Optional<Likes> findByUserAndReview(User user, Review review);
}