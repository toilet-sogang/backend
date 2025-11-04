package hwalibo.toilet.respository.review;

import hwalibo.toilet.domain.review.ReviewImage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewImageRepository extends JpaRepository<ReviewImage,Long> {

    // 1. 첫 페이지 조회
    @Query("SELECT ri FROM ReviewImage ri WHERE ri.review.toilet.id = :toiletId ORDER BY ri.id DESC")
    @EntityGraph(attributePaths = {"review", "review.toilet"})
    Slice<ReviewImage> findByToiletIdOrderByIdDesc(@Param("toiletId") Long toiletId, Pageable pageable);

    // 2. 다음 페이지 조회
    @Query("SELECT ri FROM ReviewImage ri WHERE ri.review.toilet.id = :toiletId AND ri.id < :lastPhotoId ORDER BY ri.id DESC")
    @EntityGraph(attributePaths = {"review", "review.toilet"})
    Slice<ReviewImage> findByToiletIdAndIdLessThanOrderByIdDesc(@Param("toiletId") Long toiletId, @Param("lastPhotoId") Long lastPhotoId, Pageable pageable);

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
            "LEFT JOIN FETCH r.tag t " + // Tag는 없을 수도 있으므로 LEFT JOIN
            "WHERE ri.id = :photoId")
    Optional<ReviewImage> findByIdWithReviewAndDetails(@Param("photoId") Long photoId);
}
