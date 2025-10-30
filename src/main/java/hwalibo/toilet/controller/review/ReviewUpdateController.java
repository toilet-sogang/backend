package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.request.ReviewUpdateRequest;
import hwalibo.toilet.dto.review.response.ReviewUpdateResponse;
import hwalibo.toilet.service.review.ReviewCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Review - Update")
public class ReviewUpdateController {

    private final ReviewCommandService reviewCommandService;

    @PatchMapping("/user/review/{reviewId}")
    @Operation(summary = "리뷰 수정", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<ReviewUpdateResponse>> update(@AuthenticationPrincipal User loginUser,
                                                                    @PathVariable Long reviewId,
                                                                    @RequestBody ReviewUpdateRequest request) {
        Long id = reviewCommandService.updateMyReview(loginUser, reviewId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "리뷰가 성공적으로 수정되었습니다.", ReviewUpdateResponse.of(id)));
    }
}


