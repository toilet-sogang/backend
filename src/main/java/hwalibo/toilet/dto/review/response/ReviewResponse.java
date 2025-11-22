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
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

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
    private Integer good; // '좋아요' 수 필드 추가

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @JsonProperty("isDis")
    private Boolean isDis; // ERD의 is_dis 필드에 해당


    /**
     * Review 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static ReviewResponse from(Review review,boolean canViewPhoto) {

        // ⭐ 1. User 객체의 null 여부를 확인합니다.
        // LEFT JOIN FETCH 덕분에 탈퇴 유저는 review.getUser() 호출 시 null이 반환됩니다.
        final User user = review.getUser();

        // 2. ReviewTag Enum 리스트를 String 리스트로 변환
        List<String> tagNames = review.getTag().stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        // 3. Photo URL 처리
        List<String> photoUrl=review.getReviewImages().stream()
                .sorted(Comparator.comparing(ReviewImage::getSortOrder))
                .map(ReviewImage::getUrl)
                .collect(Collectors.toList());

        if (!canViewPhoto) {
            // 다른 성별이면 사진은 제공하지 않음
            photoUrl = Collections.emptyList();
        }

        // ⭐ 4. 유저 정보 안전하게 접근 및 탈퇴 사용자 처리
        // user가 null이 아니면 실제 정보 사용, null이면 익명화된 정보 사용
        String userName = (user != null) ? user.getName() : "탈퇴한 사용자";
        String userProfile = (user != null) ? user.getProfile() : null;
        Long userId = (user != null) ? user.getId() : null;

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(userId) // 안전한 userId 사용
                .userName(userName) // 안전한 userName 사용
                .userPhoto(userProfile) // 안전한 userPhoto 사용
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