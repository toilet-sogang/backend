package hwalibo.toilet.dto.review.photo.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPhotoUpdateResponse {

    private Long photoId;
    private String newPhotoUrl;

    // 서비스에서 교체된 사진 ID와 새로 발급된 URL을 받아 DTO를 생성하는 정적 팩토리 메서드
    public static ReviewPhotoUpdateResponse of(Long photoId, String newPhotoUrl) {
        return new ReviewPhotoUpdateResponse(photoId, newPhotoUrl);
    }
}
