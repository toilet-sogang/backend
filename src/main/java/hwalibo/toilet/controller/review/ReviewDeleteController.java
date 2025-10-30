package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.service.review.ReviewCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Review - Delete")
public class ReviewDeleteController {

    private final ReviewCommandService reviewCommandService;

    @DeleteMapping("/user/review/{reviewId}")
    @Operation(summary = "리뷰 삭제", description = "내가 작성한 리뷰 삭제", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal User loginUser,
                                                    @PathVariable Long reviewId) {
        reviewCommandService.deleteMyReview(loginUser, reviewId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "리뷰가 성공적으로 삭제되었습니다."));
    }
}


