package hwalibo.toilet.dto.review.photo.response;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoReviewListResponse {

    private List<PhotoReview> content;
    private boolean hasNext;
    private String nextCursor;

    // 서비스 로직에서 페이징 처리된 Review 목록(Slice<Review>)을 받아 DTO를 생성합니다.
    public static PhotoReviewListResponse fromReviews(Slice<ReviewImage> reviewSlice,String newCursor) {
        List<PhotoReview> photoDtos = reviewSlice.getContent().stream()
                .map(image -> new PhotoReview(
                        image.getUrl(),
                        image.getReview().getId(),
                        image.getReview().getToilet().getId(),
                        image.getId()
                ))
                .collect(Collectors.toList());
        return new PhotoReviewListResponse(photoDtos, reviewSlice.hasNext(),newCursor);
    }
}
