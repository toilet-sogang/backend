package hwalibo.toilet.dto.station.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StationSuggestRequest {

    @NotNull(message = "latitude 값은 필수입니다.")
    private Double latitude;   // 사용자 현재 위도

    @NotNull(message = "longitude 값은 필수입니다.")
    private Double longitude;  // 사용자 현재 경도
}