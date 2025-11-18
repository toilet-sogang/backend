package hwalibo.toilet.controller.auth;

import hwalibo.toilet.auth.jwt.JwtConstants;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.auth.request.RefreshTokenRequest;
import hwalibo.toilet.dto.auth.response.TokenResponse;
import hwalibo.toilet.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@Slf4j
@Tag(name = "인증 (Auth)", description = "토큰 재발급, 로그아웃 등 사용자 인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    // =================================================================
    // 🔄 토큰 재발급
    // =================================================================
    @Operation(
            summary = "Access Token 재발급",
            description = "만료된 Access Token과 Refresh Token을 함께 보내 새로운 토큰들을 발급받습니다.",
            security = {}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "블랙리스트 또는 유효하지 않은 Refresh Token",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "DB에 Refresh Token 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @RequestHeader(value = JwtConstants.HEADER_STRING, required = false) String authHeader,
            @Valid @RequestBody RefreshTokenRequest request) {

        // ✅ 공통 메서드로 토큰 추출
        String accessToken = extractAccessToken(authHeader);

        TokenResponse tokenResponse = authService.reissueTokens(
                accessToken, request.getRefreshToken()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, 200, "토큰이 성공적으로 재발급되었습니다.", tokenResponse)
        );
    }

    // =================================================================
    // 🚪 로그아웃
    // =================================================================
    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 사용자를 로그아웃 처리하고 Refresh Token을 무효화합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 유효하지 않아 인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User loginUser,
            @RequestHeader(value = JwtConstants.HEADER_STRING, required = false) String authHeader
    ) {
        // ✅ 공통 메서드로 토큰 추출
        String accessToken = extractAccessToken(authHeader);

        authService.logout(loginUser, accessToken);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, HttpStatus.OK.value(), "성공적으로 로그아웃되었습니다.", null));
    }

    // =================================================================
    // 💀 회원 탈퇴
    // =================================================================
    @Operation(
            summary = "회원 탈퇴 (계정 삭제)",
            description = "현재 로그인된 사용자의 계정을 탈퇴 처리합니다. S3 이미지, 리뷰, 네이버 연동이 모두 삭제되며 토큰은 블랙리스트 처리됩니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "회원 탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawUser(
            @AuthenticationPrincipal User loginUser,
            @RequestHeader(value = JwtConstants.HEADER_STRING, required = false) String authHeader // ✅ 헤더 추가
    ) {
        // ✅ 블랙리스트 등록을 위해 현재 토큰 추출
        String accessToken = extractAccessToken(authHeader);

        // ✅ 서비스에 토큰까지 전달 (Service 수정 필요)
        authService.withdraw(loginUser, accessToken);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>(true, HttpStatus.NO_CONTENT.value(), "성공적으로 회원 탈퇴되었습니다.", null));
    }

    // =================================================================
    // 🛠️ Private Helper Method (토큰 추출 공통화)
    // =================================================================
    private String extractAccessToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(JwtConstants.TOKEN_PREFIX)) {
            return authHeader.substring(JwtConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}