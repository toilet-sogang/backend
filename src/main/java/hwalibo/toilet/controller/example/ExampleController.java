package hwalibo.toilet.controller.example;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "예시 (Example)", description = "JWT 인증이 필요 없는 예시용 API")
@RestController
public class ExampleController {

    @Operation(
            summary = "예시 엔드포인트",
            description = "JWT 인증이 필요 없는 단순 테스트용 API입니다.",
            security = {} // ✅ Swagger 문서에서 인증 요구 제거 → 🔓 표시 없어짐
    )
    @GetMapping("/example")
    public String example() {
        return "Salut, le monde!";
    }
}
