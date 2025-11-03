package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.type.SortType;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.response.ReviewListResponse;
import hwalibo.toilet.service.review.ReviewGetService;
import hwalibo.toilet.service.review.ReviewLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/toilet")
@Tag(name = "Review - Get")
public class ReviewGetController {

    private final ReviewLikeService reviewLikeService;
    private final ReviewGetService reviewGetService;

    @PostMapping("/{toiletId}/reviews/{reviewId}/like")
    @Operation(summary = "리뷰 좋아요", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<Void>> like(
            @AuthenticationPrincipal User loginUser,
            @PathVariable Long toiletId,
            @PathVariable Long reviewId) {

        reviewLikeService.like(loginUser, toiletId, reviewId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "리뷰 좋아요 성공", null));
    }

    @DeleteMapping("/{toiletId}/reviews/{reviewId}/like")
    @Operation(summary = "리뷰 좋아요 취소", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<Void>> unlike(
            @AuthenticationPrincipal User loginUser,
            @PathVariable Long toiletId,
            @PathVariable Long reviewId) {
        reviewLikeService.unlike(loginUser, toiletId, reviewId);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "리뷰 좋아요 취소 성공", null));
    }

    @GetMapping("/{toiletId}/reviews")
    @Operation(summary="특정 화장실 리뷰 목록 조회",security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<ReviewListResponse>> getReviewList(@AuthenticationPrincipal User loginUser,
                                                                         @PathVariable Long toiletId,
                                                                         @RequestParam(value = "sort", defaultValue = "LATEST") SortType sortType){
        ReviewListResponse data= reviewGetService.getReviewList(loginUser,toiletId,sortType);
        return ResponseEntity.ok(new ApiResponse<ReviewListResponse>(true,200,"리뷰 목록 조회 성공",data));
    }
}

