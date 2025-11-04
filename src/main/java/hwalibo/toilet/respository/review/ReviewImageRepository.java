package hwalibo.toilet.respository.review;

import hwalibo.toilet.domain.review.ReviewImage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewImageRepository extends JpaRepository<ReviewImage,Long> {

    // 1. 첫 페이지 조회
    @Query("SELECT ri FROM ReviewImage ri WHERE ri.review.toilet.id = :toiletId ORDER BY ri.id DESC")
    @EntityGraph(attributePaths = {"review", "review.toilet"})
    Slice<ReviewImage> findByToiletIdOrderByIdDesc(@Param("toiletId") Long toiletId, Pageable pageable);

    // 2. 다음 페이지 조회
    @Query("SELECT ri FROM ReviewImage ri WHERE ri.review.toilet.id = :toiletId AND ri.id < :lastPhotoId ORDER BY ri.id DESC")
    @EntityGraph(attributePaths = {"review", "review.toilet"})
    Slice<ReviewImage> findByToiletIdAndIdLessThanOrderByIdDesc(@Param("toiletId") Long toiletId, @Param("lastPhotoId") Long lastPhotoId, Pageable pageable);
}
