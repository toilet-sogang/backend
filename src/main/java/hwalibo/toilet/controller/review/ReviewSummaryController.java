package hwalibo.toilet.controller.review;

import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.review.response.ReviewSummaryResponse;
import hwalibo.toilet.service.review.ReviewSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "리뷰 요약 (Review Summary)", description = "특정 화장실의 리뷰를 종합하여 200바이트 이내 한국어 요약을 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/toilet")
public class ReviewSummaryController {

    private final ReviewSummaryService reviewSummaryService;

    @Operation(
            summary = "리뷰 요약 조회",
            description = "특정 화장실의 리뷰들을 취합하여 OpenAI를 통해 200바이트 이내 한국어 요약을 생성합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "리뷰 요약 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 화장실에 리뷰가 없습니다.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "리뷰 요약 생성 실패(서버 내부 오류)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/{toiletId}/reviews/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> summarize(@PathVariable Long toiletId) {
        ReviewSummaryResponse data = reviewSummaryService.summarizeByToiletId(toiletId);
        ApiResponse<ReviewSummaryResponse> response = new ApiResponse<>(
                true,
                HttpStatus.OK.value(),
                "리뷰 요약 성공",
                data
        );
        return ResponseEntity.ok(response);
    }
}
