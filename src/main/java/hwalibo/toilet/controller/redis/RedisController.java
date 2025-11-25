package hwalibo.toilet.controller.redis;

import hwalibo.toilet.dto.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Redis Test", description = "Redis 연결 상태 확인 API")
@RestController
@RequiredArgsConstructor
public class RedisController {

    private final StringRedisTemplate redisTemplate;

    @Operation(
            summary = "Redis 연결 테스트",
            description = "Redis 서버와의 연결 상태를 확인합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Redis 연결 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Redis 연결 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/redis/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        try {
            redisTemplate.opsForValue().set("ping", "pong");
            String pong = redisTemplate.opsForValue().get("ping");
            ApiResponse<String> response = new ApiResponse<>(true, 200, "Redis 연결 성공", pong);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>(false, 500, "Redis 연결 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(response); // 500 상태 코드와 함께 응답
        }
    }
}

