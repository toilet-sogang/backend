package hwalibo.toilet.dto.review.photo.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "리뷰 이미지 업로드 최종 응답 데이터")
public class PhotoUploadResponse {


    private List<PhotoUrlResponse> createdPhotos;



    public static PhotoUploadResponse of(List<PhotoUrlResponse> photoResponses) {
        return PhotoUploadResponse.builder()
                .createdPhotos(photoResponses)
                .build();
    }
}