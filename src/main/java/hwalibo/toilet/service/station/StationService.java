package hwalibo.toilet.service.station;

import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.dto.station.request.StationSuggestRequest;
import hwalibo.toilet.dto.station.response.StationSearchResponse;
import hwalibo.toilet.dto.station.response.StationSuggestResponse;
import hwalibo.toilet.respository.toilet.ToiletQueryRepository;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationService {

    private final ToiletRepository toiletRepository;
    private final ToiletQueryRepository toiletQueryRepository;

    public List<StationSearchResponse> search(String keyword) {
        List<Toilet> toilets = toiletRepository.findByNameContaining(keyword);
        return toilets.stream()
                .map(t -> StationSearchResponse.from(t, t.getStar(), t.getNumReview()))
                .collect(Collectors.toList());
    }

    public StationSuggestResponse suggest(StationSuggestRequest request) {
        double lat = request.getLatitude();
        double lng = request.getLongitude();

        List<Toilet> nearestStations = toiletQueryRepository.findTop3NearestStations(lat, lng);

        List<String> names = nearestStations.stream()
                .map(Toilet::getName)
                .collect(Collectors.toList());

        return new StationSuggestResponse(names);
    }
}

