package hwalibo.toilet.dto.review.response;

import hwalibo.toilet.domain.review.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateResponse {

    private Long reviewId;

    // Entity -> DTO 변환
    public static ReviewCreateResponse of(Review review) {
        return ReviewCreateResponse.builder()
                .reviewId(review.getId())
                .build();
    }
}