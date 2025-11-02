package hwalibo.toilet.respository.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 특정 화장실에 달린 모든 리뷰를 페이징 처리하여 조회합니다.
     * @param toilet 조회할 화장실 엔티티
     * @param pageable 페이지 번호, 페이지 당 개수, 정렬 방법 등을 담는 객체
     * @return Page<Review>
     */
    Page<Review> findAllByToilet(Toilet toilet, Pageable pageable);

    /**
     * 특정 사용자가 작성한 모든 리뷰를 조회합니다. (e.g., '내가 쓴 리뷰' 기능)
     * @param user 조회할 사용자 엔티티
     * @return List<Review>
     */
    List<Review> findAllByUser(User user);

    /**
     * 특정 사용자가 특정 화장실에 리뷰를 이미 작성했는지 확인할 때 사용합니다.
     * @param user 사용자 엔티티
     * @param toilet 화장실 엔티티
     * @return Optional<Review>
     */
    Optional<Review> findByUserAndToilet(User user, Toilet toilet);

    // toilet_id 기준으로 리뷰 정렬 조회
    List<Review> findByToiletIdOrderByCreatedAtAsc(Long toiletId);

    //count 메서드
    long countByToiletId(Long toiletId);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.reviewImages WHERE r.id = :reviewId")
    Optional<Review> findByIdWithImages(@Param("reviewId") Long reviewId);
}


