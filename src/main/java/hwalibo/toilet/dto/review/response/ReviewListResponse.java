package hwalibo.toilet.dto.review.response;

import lombok.Getter;

import java.util.List;

/**
 * 특정 화장실의 리뷰 목록 조회 API의 최종 data 필드를 구성하는 DTO입니다.
 * 리뷰 목록과 함께 추후 페이징 정보, 총 개수 등의 확장을 고려한 Wrapper 클래스입니다.
 */
@Getter
public class ReviewListResponse {

    private final List<ReviewResponse> reviews;

    public ReviewListResponse (List<ReviewResponse> reviews) {
        this.reviews = reviews;
    }
}
