package hwalibo.toilet.dto.station.response;
import lombok.Getter;

import java.util.List;

@Getter
public class StationSuggestResponse {

    private final List<String> stations;

    public StationSuggestResponse(List<String> stations) {
        this.stations = stations;
    }
}