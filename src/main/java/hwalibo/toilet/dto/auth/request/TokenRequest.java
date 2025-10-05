package hwalibo.toilet.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenRequest {

    @NotBlank(message = "액세스 토큰은 필수입니다.")
    private String accessToken;

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}
