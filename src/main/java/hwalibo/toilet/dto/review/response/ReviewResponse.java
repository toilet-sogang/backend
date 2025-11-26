package hwalibo.toilet.dto.review.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import hwalibo.toilet.domain.review.Review;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;


@Getter
@Builder
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userPhoto;
    private String description;
    private Double star;
    private List<String> tag;
    private List<String> photo;
    private Integer good;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @JsonProperty("isDis")
    private Boolean isDis;

    public static ReviewResponse from(Review review,boolean canViewPhoto) {
        final User user = review.getUser();

        List<String> tagNames = review.getTag().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        List<String> photoUrl=review.getReviewImages().stream()
                .sorted(Comparator.comparing(ReviewImage::getSortOrder))
                .map(ReviewImage::getUrl)
                .collect(Collectors.toList());

        if (!canViewPhoto) {
            photoUrl = Collections.emptyList();
        }

        String userName = (user != null) ? user.getName() : "탈퇴한 사용자";
        String userProfile = (user != null) ? user.getProfile() : null;
        Long userId = (user != null) ? user.getId() : null;

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(userId)
                .userName(userName)
                .userPhoto(userProfile)
                .description(review.getDescription())
                .star(review.getStar())
                .tag(tagNames)
                .photo(photoUrl)
                .good(review.getGood())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .isDis(review.isDis())
                .build();
    }
}