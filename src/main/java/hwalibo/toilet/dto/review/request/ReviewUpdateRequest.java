package hwalibo.toilet.dto.review.request;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.type.Tag;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {

    @NotNull(message = "별점은 필수 입력값입니다.")
    @DecimalMin(value = "0.0", inclusive = true, message = "별점은 0.0 이상이어야 합니다.")
    @DecimalMax(value = "5.0", inclusive = true, message = "별점은 5.0 이하이어야 합니다.")
    private Double star;  // 별점

    @NotBlank(message = "리뷰 내용은 비어 있을 수 없습니다.")
    @Size(max = 200, message = "리뷰 내용은 200자 이내로 작성해주세요.")
    private String description;  // 리뷰 상세 설명

    private List<Tag> tag;  // 리뷰 태그 enum 리스트 (선택값)

    @NotNull(message = "장애인 여부는 필수 입력값입니다.")
    private Boolean isDis;  // 장애인 여부

    public Review toEntity(Long reviewId) {
        return Review.builder()
                .id(reviewId)
                .star(star)
                .description(description)
                .tag(tag)
                .isDis(isDis)
                .build();
    }
}
