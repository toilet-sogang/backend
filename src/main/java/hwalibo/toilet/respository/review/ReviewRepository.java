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

    //id로 review와 reviewImage 찾기
    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.reviewImages WHERE r.id = :reviewId")
    Optional<Review> findByIdWithImages(@Param("reviewId") Long reviewId);

    /**
     * 1. 최신순 (기본값)
     * (N+1 해결을 위해 User와 ReviewImages를 한 번에 Join Fetch)
     */
    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.reviewImages " +
            "WHERE r.toilet.id = :toiletId " +
            "ORDER BY r.createdAt DESC") // 최신순 정렬
    List<Review> findByToiletId_OrderByLatest(@Param("toiletId") Long toiletId);

    /**
     * 2. 별점순
     */
    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.reviewImages " +
            "WHERE r.toilet.id = :toiletId " +
            "ORDER BY r.star DESC") // 별점순 정렬
    List<Review> findByToiletId_OrderByRating(@Param("toiletId") Long toiletId);

    /**
     * 3. 장애인 화장실 리뷰만 필터링 (isDis = true)
     * (WHERE 절을 r.isDis = true로 수정)
     */
    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.reviewImages " +
            "WHERE r.toilet.id = :toiletId AND r.isDis = true " + // [!] 이 부분 수정
            "ORDER BY r.createdAt DESC")
    // [!] 메서드 이름 변경 및 tag 파라미터 제거
    List<Review> findByToiletId_HandicappedOnly(@Param("toiletId") Long toiletId);
}


