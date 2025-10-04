package hwalibo.toilet.dto.user.response;

import hwalibo.toilet.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdatedUser {

    private Long id;
    private String name;

    public static UpdatedUser from(User user) {
        return new UpdatedUser(user.getId(), user.getName());
    }
}