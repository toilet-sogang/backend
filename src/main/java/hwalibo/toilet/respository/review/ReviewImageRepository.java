package hwalibo.toilet.respository.review;

import hwalibo.toilet.domain.review.ReviewImage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewImageRepository extends JpaRepository<ReviewImage,Long> {

    /**
     * 1. 첫 페이지 조회 (커서가 없을 때)
     * - JOIN: Review 엔티티를 JOIN하여 createdAt으로 정렬합니다.
     * - 정렬: 1순위 ri.review.createdAt DESC, 2순위 ri.id DESC
     */
    @Query("SELECT ri FROM ReviewImage ri " +
            "JOIN ri.review r " + // Review(r)를 JOIN
            "WHERE r.toilet.id = :toiletId " + // review.toilet.id로 조건 변경
            "AND ri.status='APPROVED'"+ //APPROVED 상태만 필터링
            "ORDER BY r.createdAt DESC, ri.id DESC") // 1. 정렬 기준 변경 (r.createdAt)
    @EntityGraph(attributePaths = {"review", "review.toilet"})
    Slice<ReviewImage> findFirstPageByToiletId(
            @Param("toiletId") Long toiletId,
            Pageable pageable);

    /**
     * 2. 다음 페이지 조회 (커서가 있을 때)
     * - '복합 키' WHERE 절 사용 (review.createdAt + reviewImage.id)
     */
    @Query("SELECT ri FROM ReviewImage ri " +
            "JOIN ri.review r " + // Review(r)를 JOIN
            "WHERE r.toilet.id = :toiletId " + // review.toilet.id로 조건 변경
            "AND ri.status='APPROVED'"+
            "AND (" + // 2. 복합 키 WHERE 절
            "   r.createdAt < :lastCreatedAt OR " + // 1순위: review.createdAt 비교
            "   (r.createdAt = :lastCreatedAt AND ri.id < :lastId)" + // 2순위: reviewImage.id 비교
            ") " +
            "ORDER BY r.createdAt DESC, ri.id DESC") // 3. 정렬 기준 변경 (r.createdAt)
    @EntityGraph(attributePaths = {"review", "review.toilet"})
    Slice<ReviewImage> findNextPageByToiletId(
            @Param("toiletId") Long toiletId,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt, // Review의 createdAt
            @Param("lastId") Long lastId,                       // ReviewImage의 id
            Pageable pageable);

    /**
     * N+1 문제를 해결하기 위해 @Query와 JOIN FETCH를 사용합니다.
     * 1. ri.review (Review 엔티티)
     * 2. r.user (User 엔티티)
     * 3. r.tag (Tag 컬렉션)
     * 위 3가지를 한 번의 쿼리로 모두 EAGER 로딩합니다.
     */
    @Query("SELECT ri FROM ReviewImage ri " +
            "JOIN FETCH ri.review r " +
            "JOIN FETCH r.user u " +
            "LEFT JOIN FETCH r.tag t " +
            "WHERE ri.id = :photoId " +
            "AND ri.status = 'APPROVED'")
    Optional<ReviewImage> findByIdWithReviewAndDetails(@Param("photoId") Long photoId);

    List<ReviewImage> findByReviewId(Long reviewId);
}



