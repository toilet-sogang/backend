package hwalibo.toilet.controller.review;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.chat.response.ImageStatusResponse;
import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.service.review.ReviewPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Async-Image-Inspection")
public class ReviewUploadPollingController {
    private final ReviewPostService reviewPostService;
    @Operation(
            summary = "이미지 검사 상태 폴링 API",
            description = "이미지 PENDING/APPROVED 여부를 받는다",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/api/v1/reviews/image-status")
    public ResponseEntity<ApiResponse<List<ImageStatusResponse>>> checkImageStatus(@AuthenticationPrincipal User loginUser, @RequestParam List<Long> imageIds){
        List<ImageStatusResponse> data= reviewPostService.getImageStatuses(loginUser,imageIds);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(
                        true,
                        HttpStatus.ACCEPTED.value(),
                        "이미지 폴링 성공",
                        data
                ));
    }
}
