package hwalibo.toilet.dto.review.photo.response;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.dto.review.response.ReviewTempResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoReviewDetailResponse {

    private String photoUrl;
    private ReviewTempResponse review;

    // 특정 photoUrl과 Review 엔티티를 받아 DTO를 생성
    public static PhotoReviewDetailResponse of(String photoUrl, Review review) {
        return PhotoReviewDetailResponse.builder()
                .photoUrl(photoUrl)
                .review(ReviewTempResponse.from(review)) // 기존 of() 메서드 활용
                .build();
    }
}
