package hwalibo.toilet.dto.review.photo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewPhotoUpdateResponse {
    private List<PhotoUrlResponse> photos;

    // Service Layer의 List<String> URL들을 Response DTO로 변환하는 팩토리 메서드
    public static ReviewPhotoUpdateResponse of(List<String> urls) {
        List<PhotoUrlResponse> photoResponses = urls.stream()
                // String URL -> PhotoUrlResponse 객체로 변환
                .map(url -> PhotoUrlResponse.builder().photoUrl(url).build())
                .collect(Collectors.toList());

        return ReviewPhotoUpdateResponse.builder()
                .photos(photoResponses)
                .build();
    }

}
