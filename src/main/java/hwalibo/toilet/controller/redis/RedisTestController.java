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
public class RedisTestController {

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
            // StringRedisTemplate을 통해 직접 Redis에 연결
            redisTemplate.opsForValue().set("ping", "pong"); // 테스트 키-값 저장
            String pong = redisTemplate.opsForValue().get("ping"); // 저장된 값을 읽어오기

            // Redis 연결 성공 시 200 OK 상태 코드 반환
            ApiResponse<String> response = new ApiResponse<>(true, 200, "Redis 연결 성공", pong);
            return ResponseEntity.ok(response); // 200 OK와 함께 응답

        } catch (Exception e) {
            // Redis 연결 실패 시 500 상태 코드 반환
            ApiResponse<String> response = new ApiResponse<>(false, 500, "Redis 연결 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(response); // 500 상태 코드와 함께 응답
        }
    }
}

