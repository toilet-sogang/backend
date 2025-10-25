package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.response.ReviewListResponse;
import hwalibo.toilet.service.review.ReviewService;
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

@Tag(name = "리뷰(Review)", description = "리뷰 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/toilet")
public class ReviewListController {
    private final ReviewService reviewService;
    @Operation(
            summary = "리뷰 목록 조회",
            description = "특정 화장실의 상세 정보 조회.",
            security = { @SecurityRequirement(name = "bearerAuth") } // ✅ JWT 필요 → 🔒 표시됨
    )
    @GetMapping("{/id}/reviews")
    public ResponseEntity<ApiResponse<ReviewListResponse>> reviewList(
            @AuthenticationPrincipal User loginUser, @PathVariable("id") Long toiletId){

    }
}
