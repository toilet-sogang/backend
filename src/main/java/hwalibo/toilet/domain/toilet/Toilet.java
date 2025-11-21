package hwalibo.toilet.domain.toilet;

import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.InOut;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Toilet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 화장실 이름

    @Column(nullable = false)
    private Integer line; // 지하철 호선

    @Enumerated(EnumType.STRING)
    private Gender gender; // 사용 가능 성별

    @Column(columnDefinition = "DOUBLE DEFAULT 0.0")
    private Double star; // 평균 별점

    @Column(nullable = false)
    private Double latitude; // 위도

    @Column(nullable = false)
    private Double longitude; // 경도

    private Integer numBigToilet; // 대변기 수

    private Integer numSmallToilet; // 소변기 수

    @Column(nullable = false)
    private Integer numGate; // 출입구 번호

    @Enumerated(EnumType.STRING)
    private InOut inOut;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer numReview; // 리뷰 개수

    //별점 및 리뷰 갯수 갱신 함수
    public void updateReviewStats(double newReviewStar) {

        // 1. (변경점) null 체크 및 0으로 초기화
        // DB에서 가져온 값이 null일 경우를 대비해 0.0과 0으로 처리합니다.
        double currentStar = (this.star != null) ? this.star : 0.0;
        int currentNumReview = (this.numReview != null) ? this.numReview : 0;

        // 2. 기존 로직 수행
        // (안전하게 초기화된 currentStar, currentNumReview 사용)
        double oldTotalStars = currentStar * currentNumReview;
        int newNumReview = currentNumReview + 1;
        double newAverageStar = (oldTotalStars + newReviewStar) / newNumReview;

        // 3. 값 갱신
        this.star = newAverageStar;
        this.numReview = newNumReview;
    }

    public void removeReviewStats(double removedReviewStar) {
        double currentStar = (this.star != null) ? this.star : 0.0;
        int currentNumReview = (this.numReview != null) ? this.numReview : 0;

        if (currentNumReview <= 1) {
            this.star = 0.0;
            this.numReview = 0;
            return;
        }

        double totalStars = currentStar * currentNumReview;
        int newNumReview = currentNumReview - 1;
        double newAverageStar = (totalStars - removedReviewStar) / newNumReview;

        this.star = newAverageStar;
        this.numReview = newNumReview;
    }
}
