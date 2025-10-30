package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.response.MyReviewListResponse;
import hwalibo.toilet.service.review.ReviewQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/review")
@Tag(name = "Review - My List")
public class ReviewMyListController {

    private final ReviewQueryService reviewQueryService;

    @GetMapping("/list")
    @Operation(summary = "내가 쓴 리뷰 모아보기", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<MyReviewListResponse>> list(@AuthenticationPrincipal User loginUser) {
        MyReviewListResponse data = reviewQueryService.getMyReviews(loginUser);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "리뷰 목록을 성공적으로 조회했습니다.", data));
    }
}


