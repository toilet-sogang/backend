package hwalibo.toilet.dto.user.response;

import hwalibo.toilet.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateResponse {

    private String name;

    // ✅ 정적 팩토리 메서드
    public static UserUpdateResponse from(User user) {
        return UserUpdateResponse.builder()
                .name(user.getName())
                .build();
    }
}

