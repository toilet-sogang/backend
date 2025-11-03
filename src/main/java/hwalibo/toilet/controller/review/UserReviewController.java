package hwalibo.toilet.controller.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.photo.request.ReviewPhotoUpdateRequest;
import hwalibo.toilet.dto.review.photo.response.ReviewPhotoUpdateResponse;
import hwalibo.toilet.dto.review.request.ReviewUpdateRequest;
import hwalibo.toilet.dto.review.response.MyReviewListResponse;
import hwalibo.toilet.dto.review.response.ReviewUpdateResponse;
import hwalibo.toilet.service.review.ReviewCommandService;
import hwalibo.toilet.service.review.ReviewQueryService;
import hwalibo.toilet.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.channels.MulticastChannel;
import java.util.List;

@RestController
@RequestMapping("/user/review")
@RequiredArgsConstructor
@Tag(name = "User Review")
public class UserReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

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

    @PatchMapping(value="/{reviewId}/images",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary="리뷰 이미지 수정", description="내가 리뷰 속 이미지 수정하기",security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<ReviewPhotoUpdateResponse>> updateImage(@AuthenticationPrincipal User loginUser,
                                                                              @PathVariable Long reviewId,
                                                                              @Schema(description = "삭제할 ID 정보 (JSON)")
                                                                                  @RequestPart(value = "request", required = false) String requestJson, @Valid @Size(min=0,max=2)@RequestPart(value="images",required = false) List<MultipartFile> images) throws java.io.IOException{
        //swagger가 json이 아니라 string으로 인식하기에 string으로 받고 json에서 dto로 변환
        ReviewPhotoUpdateRequest request;
        if(requestJson != null) {
            try {
                request = objectMapper.readValue(requestJson, ReviewPhotoUpdateRequest.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("잘못된 JSON 형식의 요청입니다.");
            }
        }else request=null;

        ReviewPhotoUpdateResponse data=userService.updateImage(loginUser,reviewId,request,images);
        return ResponseEntity.ok(new ApiResponse<ReviewPhotoUpdateResponse>(true,200,"이미지가 성공적으로 수정되었습니다.",data));
    }

}
