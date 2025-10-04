package hwalibo.toilet.dto.review.response;

import lombok.Getter;

import java.util.List;

/**
 * '내가 쓴 리뷰 모아보기' API의 최종 data 필드를 구성하는 DTO입니다.
 * 리뷰 목록과 함께 추후 페이징 정보, 총 개수 등의 확장을 고려한 Wrapper 클래스입니다.
 */
@Getter
public class MyReviewListResponse {

    private final List<MyReviewResponse> reviews;
    // private final int totalCount; // 예시: 나중에 총 리뷰 개수 등 필드를 손쉽게 추가할 수 있습니다.

    public MyReviewListResponse(List<MyReviewResponse> reviews) {
        this.reviews = reviews;
    }
}
