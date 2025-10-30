package hwalibo.toilet.dto.review.photo.response;

import hwalibo.toilet.domain.review.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoReviewListResponse {

    private List<PhotoReview> content;
    private boolean hasNext;

    // 서비스 로직에서 페이징 처리된 Review 목록(Slice<Review>)을 받아 DTO를 생성합니다.
    public static PhotoReviewListResponse fromReviews(Slice<Review> reviewSlice) {
        List<PhotoReview> photoDtos = new ArrayList<>();

        // 현재 Review 엔티티에 사진 필드가 없어 빈 리스트로 반환합니다.
        reviewSlice.getContent(); // no-op

        return PhotoReviewListResponse.builder()
                .content(photoDtos)
                .hasNext(reviewSlice.hasNext())
                .build();
    }
}


