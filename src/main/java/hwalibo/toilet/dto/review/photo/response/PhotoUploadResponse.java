package hwalibo.toilet.dto.review.photo.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoUploadResponse {

    private List<String> createdPhotoUrls;

    // Service Layer에서 생성된 URL 리스트를 받아 DTO를 생성합니다.
    public static PhotoUploadResponse of(List<String> urls) {
        return PhotoUploadResponse.builder()
                .createdPhotoUrls(urls)
                .build();
    }
}