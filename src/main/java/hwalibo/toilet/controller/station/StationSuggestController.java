package hwalibo.toilet.controller.station;

import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.station.request.StationSuggestRequest;
import hwalibo.toilet.service.station.StationSuggestService;
import hwalibo.toilet.dto.station.response.StationSuggestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/station")
@Tag(name = "Station - Suggest")
public class StationSuggestController {

    private final StationSuggestService stationSuggestService;

    public StationSuggestController(StationSuggestService stationSuggestService) {
        this.stationSuggestService = stationSuggestService;
    }

    @PostMapping("/suggest")
    @Operation(summary = "역 이름 자동완성")
    public ResponseEntity<ApiResponse<StationSuggestResponse>> suggest(@RequestBody StationSuggestRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, 400, "latitude와 longtitude 값이 필요합니다.", null));
        }
        StationSuggestResponse data = stationSuggestService.suggest(request);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "가까운 역 3개 반환 성공", data));
    }
}


