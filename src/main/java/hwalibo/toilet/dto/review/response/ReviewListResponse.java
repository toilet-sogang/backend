package hwalibo.toilet.dto.review.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ReviewListResponse {

    private final List<ReviewResponse> reviews;

    public ReviewListResponse (List<ReviewResponse> reviews) {
        this.reviews = reviews;
    }
}
