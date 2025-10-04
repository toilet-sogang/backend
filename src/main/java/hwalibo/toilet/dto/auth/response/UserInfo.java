package hwalibo.toilet.dto.auth.response;

import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserInfo {
    private Long id;
    private String name;
    private String gender;
    private String profile;

    // User 엔티티를 UserDto로 변환하는 정적 메서드
    public static UserInfo from(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .gender(user.getGender().name())
                .profile(user.getProfile())
                .build();
    }
}