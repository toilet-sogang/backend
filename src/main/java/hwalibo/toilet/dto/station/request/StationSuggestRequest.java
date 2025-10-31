package hwalibo.toilet.dto.station.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StationSuggestRequest {
    private double latitude;   // 사용자 현재 위도
    private double longitude;  // 사용자 현재 경도
}