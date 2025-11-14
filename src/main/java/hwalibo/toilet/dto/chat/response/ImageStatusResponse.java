package hwalibo.toilet.dto.chat.response;

import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.type.ValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class ImageStatusResponse {
    private Long imageId;
    private ValidationStatus status;

    public ImageStatusResponse(ReviewImage image) {
        this.imageId = image.getId();
        this.status = image.getStatus();
    }
}
