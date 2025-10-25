package hwalibo.toilet.controller.toilet;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.toilet.response.ToiletDetailResponse;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import hwalibo.toilet.service.toilet.ToiletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "화장실 상세 정보 조회", description = " 특정 화장실의 상세 정보 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/toilet")
public class ToiletController {
    private final ToiletService toiletService;

    @Operation(
            summary = "화장실 상세 정보 조회",
            description = "특정 화장실의 상세 정보 조회",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ToiletDetailResponse>> toiletDetail(
            @AuthenticationPrincipal User loginUser,@PathVariable("id") Long toiletId)
            {

            ToiletDetailResponse response=toiletService.getToiletDetail(loginUser,toiletId);

            return ResponseEntity.ok(new ApiResponse<>(true, 200, "화장실 상세 조회 성공", response));
    }
}
