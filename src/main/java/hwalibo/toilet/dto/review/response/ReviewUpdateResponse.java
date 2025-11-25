package hwalibo.toilet.dto.review.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateResponse {
    private Long reviewId;
    // 서비스 계층에서 수정된 review의 ID를 받아 DTO를 생성하는 정적 팩토리 메서드
    public static ReviewUpdateResponse of(Long reviewId) {
        return new ReviewUpdateResponse(reviewId);
    }
}