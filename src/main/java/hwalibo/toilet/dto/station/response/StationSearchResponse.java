package hwalibo.toilet.dto.station.response;

import hwalibo.toilet.domain.toilet.Toilet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StationSearchResponse {

    private final Long id;
    private final String name;
    private final Integer line;
    private final String gender;
    private final Double star;
    private final Integer numReview;

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