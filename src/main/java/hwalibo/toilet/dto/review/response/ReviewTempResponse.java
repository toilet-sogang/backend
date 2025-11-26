package hwalibo.toilet.dto.review.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import hwalibo.toilet.domain.review.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ReviewTempResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String description;
    private Double star;
    private List<String> tag;
    private Integer good;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonProperty("isDis")
    private Boolean isDis;

    public static ReviewTempResponse from(Review review) {
        List<String> tagNames = review.getTag().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        return ReviewTempResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .description(review.getDescription())
                .star(review.getStar())
                .tag(tagNames)
                .good(review.getGood())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .isDis(review.isDis())
                .build();
    }
}
