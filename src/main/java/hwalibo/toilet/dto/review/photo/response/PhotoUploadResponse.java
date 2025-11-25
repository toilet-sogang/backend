package hwalibo.toilet.dto.review.photo.response;

import hwalibo.toilet.domain.review.ReviewImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "리뷰 이미지 업로드 최종 응답 데이터")
public class PhotoUploadResponse {

    // [!] 필드명을 명세서의 키인 "createdPhotos"에 맞춥니다.
    private List<PhotoUrlResponse> createdPhotos;


    // Service Layer의 List<String> URL들을 Response DTO로 변환하는 팩토리 메서드
    public static PhotoUploadResponse of(List<PhotoUrlResponse> photoResponses) {
        return PhotoUploadResponse.builder()
                .createdPhotos(photoResponses)
                .build();
    }
}