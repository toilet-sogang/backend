package hwalibo.toilet.dto.review.photo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoReview {

    private String photoUrl;
    private Long reviewId;
    private Long toiletId;

}