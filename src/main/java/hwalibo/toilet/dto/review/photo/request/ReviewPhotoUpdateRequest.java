package hwalibo.toilet.dto.review.photo.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@NoArgsConstructor
public class ReviewPhotoUpdateRequest {
    private MultipartFile file; // 업로드할 새 이미지
}
