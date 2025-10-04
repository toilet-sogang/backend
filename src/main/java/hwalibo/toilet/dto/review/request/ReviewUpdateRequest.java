package hwalibo.toilet.dto.review.request;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.type.Tag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {
    private Double star;           // 별점
    private String description;           // 리뷰 상세 설명
    private List<Tag> tag;     // 리뷰 태그 enum 리스트
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
