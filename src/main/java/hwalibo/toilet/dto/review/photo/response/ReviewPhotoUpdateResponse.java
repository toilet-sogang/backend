package hwalibo.toilet.dto.review.photo.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewPhotoUpdateResponse {
    private List<UpdatedPhotoDto> photos;

    public static ReviewPhotoUpdateResponse of(List<UpdatedPhotoDto> photos) {
        return ReviewPhotoUpdateResponse.builder()
                .photos(photos)
                .build();
    }

    @Getter
    @Builder
    public static class UpdatedPhotoDto {
        private int index;
        private Long imageId;
    }
}
