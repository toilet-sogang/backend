package hwalibo.toilet.dto.station.response;

import hwalibo.toilet.domain.toilet.Toilet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StationSearchResponse {

    private final Long id;          // 화장실 ID
    private final String name;        // 역 이름 (e.g., "신촌(지하)")
    private final Integer line;       // 호선
    private final String gender;      // 성별 ("F" / "M")
    private final Double star;        // 평균 별점
    private final Integer numReview;  // 리뷰 개수

    // Toilet Entity를 DTO로 변환하는 정적 팩토리 메서드
    public static StationSearchResponse from(Toilet toilet, Double averageStar, Integer reviewCount) {
        return StationSearchResponse.builder()
                .id(toilet.getId())
                .name(toilet.getName())
                .line(toilet.getLine())
                .gender(toilet.getGender().name())
                .star(averageStar != null ? averageStar : toilet.getStar())
                .numReview(reviewCount != null ? reviewCount : toilet.getNumReview())
                .build();
    }
}