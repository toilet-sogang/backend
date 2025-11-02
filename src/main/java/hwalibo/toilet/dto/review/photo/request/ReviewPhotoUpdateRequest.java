package hwalibo.toilet.dto.review.photo.request;

import lombok.Data;
import java.util.List;

@Data
public class ReviewPhotoUpdateRequest {
    private List<Long> deletedImageIds;
}