package hwalibo.toilet.dto.review.photo.response;

import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.type.ValidationStatus;
import hwalibo.toilet.service.user.UserService;
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
    private List<UpdatedPhotoDto> photos;

    // 그냥 리스트를 받는 생성자 (Builder가 처리)
    public static ReviewPhotoUpdateResponse of(List<UpdatedPhotoDto> photos) {
        return ReviewPhotoUpdateResponse.builder()
                .photos(photos)
                .build();
    }

    @Getter
    @Builder
    public static class UpdatedPhotoDto {
        private int index;
        private ValidationStatus status;
    }
}
