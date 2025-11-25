package hwalibo.toilet.controller.user;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.user.request.UserNameUpdateRequest;
import hwalibo.toilet.dto.user.response.UserResponse;
import hwalibo.toilet.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 (User)", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/profile")
public class UserController {
    private final UserService userService;

    //내 정보 조회
    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인된 사용자의 정보를 조회합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> profile(@AuthenticationPrincipal User loginUser) {
        UserResponse userInfo = userService.getUserInfo(loginUser);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "내 정보 조회 성공", userInfo));
    }

    //유저 이름 수정
    @Operation(
            summary = "유저 이름 수정",
            description = "로그인한 사용자의 이름을 수정합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PatchMapping("/name")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserName(@AuthenticationPrincipal User loginUser, @Valid @RequestBody UserNameUpdateRequest request
    ) {
        UserResponse response = userService.updateUserName(loginUser, request);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "사용자 이름이 성공적으로 수정되었습니다.", response));
    }
}