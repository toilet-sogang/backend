package hwalibo.toilet.dto.review.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.type.Tag;
import hwalibo.toilet.domain.user.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateRequest {

    @Size(max = 200, message = "리뷰 내용은 최대 200자까지 입력 가능합니다.")
    private String description;

    @NotNull(message = "별점은 필수 입력값입니다.")
    @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 5.0 이하이어야 합니다.")
    private Double star;

    @Size(max=3,message="태그는 최대 3개까지만 선택할 수 있습니다.")
    private List<String> tag;

    @NotNull(message="장애인 화장실 여부는 필수 선택해야합니다.")
    @JsonProperty("isDis")
    private boolean isDis;
    // DTO → Entity 변환
    // 실제 서비스 로직에서는 이 메서드로 생성된 Review 객체에
    // User와 Toilet 엔티티를 추가로 설정해주어야 합니다.
    public Review toEntity(User user, Toilet toilet) {
        List<Tag>tagEnum=(this.tag!=null)?
                this.tag.stream()
                        .map(Tag::valueOf)
                        .collect(Collectors.toList()):
                new ArrayList<>();

        return Review.builder()
                .description(this.description)
                .star(this.star)
                .user(user)
                .toilet(toilet)
                .good(0)
                .tag(tagEnum)
                .isDis(isDis)
                .build();
    }
}