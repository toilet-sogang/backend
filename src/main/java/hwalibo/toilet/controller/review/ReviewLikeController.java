package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.service.review.ReviewLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewLikeController {
    private final ReviewLikeService reviewLikeService;

    @PostMapping("/{toiletId}/reviews/{reviewId}/like")
    @Operation(summary = "리뷰 좋아요", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<Void>> like(
            @AuthenticationPrincipal User loginUser,
            @PathVariable Long toiletId,
            @PathVariable Long reviewId) {

        reviewLikeService.like(loginUser, toiletId, reviewId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        HttpStatus.CREATED.value(),
                        "리뷰 좋아요 성공",
                        null
                ));
    }

    @DeleteMapping("/{toiletId}/reviews/{reviewId}/like")
    @Operation(summary = "리뷰 좋아요 취소", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<Void>> unlike(
            @AuthenticationPrincipal User loginUser,
            @PathVariable Long toiletId,
            @PathVariable Long reviewId) {

        reviewLikeService.unlike(loginUser, toiletId, reviewId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>(
                        true,
                        HttpStatus.NO_CONTENT.value(),
                        "리뷰 좋아요 취소 성공",
                        null
                ));
    }
}
