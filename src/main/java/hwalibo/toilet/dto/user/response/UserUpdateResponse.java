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

    private Long id;
    private String name;

    // ✅ 정적 팩토리 메서드
    public static UserUpdateResponse from(User user) {
        return UserUpdateResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}

