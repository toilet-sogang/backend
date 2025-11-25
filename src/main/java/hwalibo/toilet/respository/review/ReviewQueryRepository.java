package hwalibo.toilet.respository.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewQueryRepository extends JpaRepository<Review, Long> {
    /**
     * 사용자가 작성한 리뷰 목록을 조회합니다. (내가 쓴 리뷰)
     * * 1. N+1 문제 해결을 위해 ReviewImages를 LEFT JOIN FETCH 합니다.
     * 2. 사진이 없는 리뷰 (ri IS NULL)와 APPROVED 상태의 사진이 있는 리뷰만 포함합니다.
     * 3. DISTINCT를 사용하여 JOIN FETCH로 인해 발생할 수 있는 Review 엔티티의 중복을 제거합니다.
     * (GROUP BY를 제거하여 'only_full_group_by' 에러를 회피합니다.)
     */
    @Query("SELECT DISTINCT r FROM Review r " +
            "LEFT JOIN FETCH r.reviewImages ri " +
            "WHERE r.user = :user " +
            "AND (ri IS NULL OR ri.status = 'APPROVED') " + // ri IS NULL을 포함하여 사진이 없는 리뷰도 조회
            "ORDER BY r.createdAt DESC") // 최신순 정렬
    List<Review> findAllByUser(@Param("user") User user);

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
            "WHERE r.toilet.id = :toiletId AND r.isDis = true " +
            "ORDER BY r.createdAt DESC")
    List<Review> findByToiletId_HandicappedOnly(@Param("toiletId") Long toiletId);
}
