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
    private Boolean isDis; // ERD의 is_dis 필드에 해당

    /**
     * Review 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
    // [수정] 메서드의 반환 타입을 클래스 이름과 일치시켰습니다.
    public static ReviewTempResponse from(Review review) {
        // ReviewTag Enum 리스트를 String 리스트로 변환
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
