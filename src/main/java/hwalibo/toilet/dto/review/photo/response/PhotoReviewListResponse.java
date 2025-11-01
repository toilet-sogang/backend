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

    // 서비스 로직에서 페이징 처리된 Review 목록(Slice<Review>)을 받아 DTO를 생성합니다.
    public static PhotoReviewListResponse fromReviews(Slice<Review> reviewSlice) {
        List<PhotoReview> photoDtos = new ArrayList<>();


        // 각 리뷰에 포함된 사진 URL들을 별도의 PhotoReviewDto 객체로 풀어주는 과정
        reviewSlice.getContent().forEach(review -> {
            review.getReviewImages().stream()
                    .sorted(Comparator.comparing(ReviewImage::getSortOrder))
                    .forEach(reviewImage -> {
                        photoDtos.add(PhotoReview.builder()
                                .photoUrl(reviewImage.getUrl())
                                .reviewId(review.getId())
                                .toiletId(review.getToilet().getId())
                                .build());
                    });
            });

        return PhotoReviewListResponse.builder()
                .content(photoDtos)
                .hasNext(reviewSlice.hasNext())
                .build();
    }
}

