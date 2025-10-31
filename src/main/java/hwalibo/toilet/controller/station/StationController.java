package hwalibo.toilet.controller.station;

import hwalibo.toilet.dto.global.response.ApiResponse;
import hwalibo.toilet.dto.station.request.StationSuggestRequest;
import hwalibo.toilet.dto.station.response.StationSearchResponse;
import hwalibo.toilet.dto.station.response.StationSuggestResponse;
import hwalibo.toilet.service.station.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/station")
@RequiredArgsConstructor
@Tag(name = "Station")
public class StationController {

    private final StationService stationService;

    @GetMapping("/search")
    @Operation(summary = "역 검색 결과 조회", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<ApiResponse<List<StationSearchResponse>>> search(@RequestParam("q") String q) {
        List<StationSearchResponse> data = stationService.search(q);
        String message = data.isEmpty() ? "검색 결과가 없습니다." : "검색 성공";
        return ResponseEntity.ok(new ApiResponse<>(true, 200, message, data));
    }

    @PostMapping("/suggest")
    @Operation(summary = "역 이름 자동완성")
    public ResponseEntity<ApiResponse<StationSuggestResponse>> suggest(@RequestBody StationSuggestRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, 400, "latitude와 longtitude 값이 필요합니다.", null));
        }
        StationSuggestResponse data = stationService.suggest(request);
        return ResponseEntity.ok(new ApiResponse<>(true, 200, "가까운 역 3개 반환 성공", data));
    }
}
