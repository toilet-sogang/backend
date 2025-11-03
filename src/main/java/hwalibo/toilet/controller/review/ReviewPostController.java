package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.photo.response.PhotoUploadResponse;
import hwalibo.toilet.service.review.ReviewPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


@Tag(name="Review Upload",description="리뷰 업로드 관련 API")
@RequestMapping("/toilet")
@RestController
@RequiredArgsConstructor
@Validated
public class ReviewPostController {
    private final ReviewPostService reviewPostService;

    @Operation(
            summary="리뷰 이미지 업로드",
            description="리뷰 이미지 작성하기",
            security={ @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping(value="/{reviewId}/images", consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadImage(@AuthenticationPrincipal User loginUser,
                                                                        @PathVariable Long reviewId,
                                                                        @Valid@Size(min=0,max=2) @RequestPart("images")   List<MultipartFile> images){
        PhotoUploadResponse data= reviewPostService.uploadImage(loginUser,reviewId,images);
        return ResponseEntity.ok(new ApiResponse<PhotoUploadResponse>(true, HttpStatus.CREATED.value(),  "이미지 업로드 성공",data));

    }
}
