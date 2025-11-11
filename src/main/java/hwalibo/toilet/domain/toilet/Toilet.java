package hwalibo.toilet.domain.toilet;

import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.InOut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
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
    public void updateReviewStats(double newReviewStar){
        double oldTotalStars=this.star*this.numReview;
        this.numReview++;

        this.star=(oldTotalStars+newReviewStar)/this.numReview;
    }
}
