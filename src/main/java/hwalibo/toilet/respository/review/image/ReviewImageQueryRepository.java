package hwalibo.toilet.respository.review.image;

import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.type.Gender;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReviewImageQueryRepository extends JpaRepository<ReviewImage,Long> {
    /**
     * 1. 첫 페이지 조회 (커서가 없을 때)
     * - 성별 필터링 추가: 로그인 유저의 성별과 화장실 성별이 일치하는 리뷰만 조회
     */
    @Query("SELECT ri FROM ReviewImage ri " +
            "JOIN ri.review r " +
            "WHERE r.toilet.id = :toiletId " +
            "AND r.toilet.gender = :gender " +
            "AND ri.status='APPROVED'" +
            "ORDER BY r.createdAt DESC, ri.id DESC")
    @EntityGraph(attributePaths = {"review", "review.toilet"})
    Slice<ReviewImage> findFirstPageByToiletId(
            @Param("toiletId") Long toiletId,
            @Param("gender") Gender gender,
            Pageable pageable);

    /**
     * 2. 다음 페이지 조회 (커서가 있을 때)
     * - 성별 필터링 추가: 로그인 유저의 성별과 화장실 성별이 일치하는 리뷰만 조회
     */
    @Query("SELECT ri FROM ReviewImage ri " +
            "JOIN ri.review r " +
            "WHERE r.toilet.id = :toiletId " +
            "AND r.toilet.gender = :gender " +
            "AND ri.status='APPROVED'" +
            "AND (" +
            "   r.createdAt < :lastCreatedAt OR " +
            "   (r.createdAt = :lastCreatedAt AND ri.id < :lastId)" +
            ") " +
            "ORDER BY r.createdAt DESC, ri.id DESC")
    @EntityGraph(attributePaths = {"review", "review.toilet"})
    Slice<ReviewImage> findNextPageByToiletId(
            @Param("toiletId") Long toiletId,
            @Param("gender") Gender gender,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable);

    /**
     * N+1 문제를 해결하기 위해 @Query와 JOIN FETCH를 사용합니다.
     * 이 메서드는 상세 조회용이며, 성별 필터링 로직은 ReviewGetService에서 처리됩니다. (유지)
     */
    @Query("SELECT ri FROM ReviewImage ri " +
            "JOIN FETCH ri.review r " +
            "JOIN FETCH r.user u " +
            "LEFT JOIN FETCH r.tag t " +
            "WHERE ri.id = :photoId " +
            "AND ri.status = 'APPROVED'" )
    Optional<ReviewImage> findByIdWithReviewAndDetails(@Param("photoId") Long photoId);
}
