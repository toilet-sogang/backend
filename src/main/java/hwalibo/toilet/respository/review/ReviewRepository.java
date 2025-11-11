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


    @Query("SELECT DISTINCT r FROM Review r " +
            "LEFT JOIN FETCH r.reviewImages ri " +
            "WHERE r.user = :user ")
    List<Review> findAllByUser(@Param("user") User user);


    // toilet_id 기준으로 리뷰 정렬 조회
    List<Review> findByToiletIdOrderByCreatedAtAsc(Long toiletId);


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


