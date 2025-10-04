package hwalibo.toilet.dto.user.response;

import hwalibo.toilet.domain.user.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private String name;      // 닉네임
    private String profile;   // 프로필 이미지 URL
    private int rate;         // 상위 퍼센트 (정수)
    private int numReview;    // 리뷰 개수

    public static UserResponse from(User user, int rate) {
        return UserResponse.builder()
                .name(user.getName())
                .profile(user.getProfile())
                .rate(rate)
                .numReview(user.getNumReview() != null ? user.getNumReview() : 0)
                .build();
    }
}

