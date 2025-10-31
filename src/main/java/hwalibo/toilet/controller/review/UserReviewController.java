package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.request.ReviewUpdateRequest;
import hwalibo.toilet.dto.review.response.MyReviewListResponse;
import hwalibo.toilet.dto.review.response.ReviewUpdateResponse;
import hwalibo.toilet.service.review.ReviewCommandService;
import hwalibo.toilet.service.review.ReviewQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/review")
@RequiredArgsConstructor
@Tag(name = "User Review")
public class UserReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    @GetMapping("/list")
    @Operation(summary = "내가 쓴 리뷰 모아보기", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<MyReviewListResponse>> list(@AuthenticationPrincipal User loginUser) {
        MyReviewListResponse data = reviewQueryService.getMyReviews(loginUser);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "리뷰 목록을 성공적으로 조회했습니다.", data));
    }

    @PatchMapping("/{reviewId}")
    @Operation(summary = "리뷰 수정", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<ReviewUpdateResponse>> update(@AuthenticationPrincipal User loginUser,
                                                                    @PathVariable Long reviewId,
                                                                    @RequestBody ReviewUpdateRequest request) {
        Long id = reviewCommandService.updateMyReview(loginUser, reviewId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "리뷰가 성공적으로 수정되었습니다.", ReviewUpdateResponse.of(id)));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "리뷰 삭제", description = "내가 작성한 리뷰 삭제", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal User loginUser,
                                                    @PathVariable Long reviewId) {
        reviewCommandService.deleteMyReview(loginUser, reviewId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "리뷰가 성공적으로 삭제되었습니다."));
    }
}

