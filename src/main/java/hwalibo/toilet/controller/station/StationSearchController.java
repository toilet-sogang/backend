package hwalibo.toilet.controller.station;

import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.station.response.StationSearchResponse;
import hwalibo.toilet.service.station.StationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/station")
@Tag(name = "Station - Search")
public class StationSearchController {

    private final StationSearchService stationSearchService;

    public StationSearchController(StationSearchService stationSearchService) {
        this.stationSearchService = stationSearchService;
    }

    @GetMapping("/search")
    @Operation(summary = "역 검색 결과 조회")
    public ResponseEntity<ApiResponse<List<StationSearchResponse>>> search(@RequestParam("q") String q) {
        List<StationSearchResponse> data = stationSearchService.search(q);
        String message = data.isEmpty() ? "검색 결과가 없습니다." : "검색 성공";
        return ResponseEntity.ok(new ApiResponse<>(true, 200, message, data));
    }
}


