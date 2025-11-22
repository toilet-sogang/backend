package hwalibo.toilet.dto.toilet.response;

import hwalibo.toilet.domain.toilet.Toilet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToiletDetailResponse {

    private Long id;
    private String name;
    private int line;
    private String gender;
    private double star;
    private int numBigToilet;
    private int numSmallToilet;
    private int numGate;
    private String isIn; // "In" or "Out"
    private double latitude;
    private double longitude;
    private int numReview;

    // Entity → DTO 변환
    public static ToiletDetailResponse of(Toilet toilet) {
        double formattedStar = Double.parseDouble(
                String.format("%.1f", toilet.getStar())
        );

        return ToiletDetailResponse.builder()
                .id(toilet.getId())
                .name(toilet.getName())
                .line(toilet.getLine())
                .gender(String.valueOf(toilet.getGender()))
                .star(formattedStar)
                .numBigToilet(toilet.getNumBigToilet())
                .numSmallToilet(toilet.getNumSmallToilet())
                .numGate(toilet.getNumGate())
                .isIn(String.valueOf(toilet.getInOut()))
                .latitude(toilet.getLatitude())
                .longitude(toilet.getLongitude())
                .numReview(toilet.getNumReview())
                .build();
    }
}
