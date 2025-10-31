package hwalibo.toilet.dto.review.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import hwalibo.toilet.domain.review.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/*@Getter
@Builder
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String description;
    private Double star;
    private List<String> tag;
    private List<String> photo;
    private Integer good; // '좋아요' 수 필드 추가

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Boolean isDis; // ERD의 is_dis 필드에 해당

    *//**
     * Review 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     *//*
    public static ReviewResponse from(Review review) {
        // ReviewTag Enum 리스트를 String 리스트로 변환
        List<String> tagNames = review.getTag().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .description(review.getDescription())
                .star(review.getStar())
                .tag(tagNames)
                .photo(review.getPhoto())
                .good(review.getGood())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .isDis(review.isDis())
                .build();
    }
}*/

