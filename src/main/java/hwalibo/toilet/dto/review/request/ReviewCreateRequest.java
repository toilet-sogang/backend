package hwalibo.toilet.dto.review.request;

import hwalibo.toilet.domain.review.Review;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateRequest {

    @NotBlank(message = "리뷰 내용은 필수 입력값입니다.")
    private String description;

    @NotNull(message = "별점은 필수 입력값입니다.")
    @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 5.0 이하이어야 합니다.")
    private Double star;

    private List<String> tag;

    // DTO → Entity 변환
    // 실제 서비스 로직에서는 이 메서드로 생성된 Review 객체에
    // User와 Toilet 엔티티를 추가로 설정해주어야 합니다.
    public Review toEntity() {
        return Review.builder()
                .description(this.description)
                .star(this.star)
                .build();
    }
}