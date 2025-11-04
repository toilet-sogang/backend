package hwalibo.toilet.dto.review.photo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "업로드된 사진 하나의 URL 정보")
public class PhotoUrlResponse {

    // JSON의 키인 "photoUrl"과 일치하도록 필드명을 정합니다.
    private String photoUrl;
    private Long photoId;
}