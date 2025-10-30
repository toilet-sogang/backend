package hwalibo.toilet.service.station;

import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.dto.station.request.StationSuggestRequest;
import hwalibo.toilet.dto.station.response.StationSuggestResponse;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StationSuggestService {

    private final ToiletRepository toiletRepository;

    public StationSuggestService(ToiletRepository toiletRepository) {
        this.toiletRepository = toiletRepository;
    }

    public StationSuggestResponse suggest(StationSuggestRequest request) {
        double lat = request.getLatitude();
        double lng = request.getLongitude();

        List<String> names = toiletRepository.findAll().stream()
                .sorted((a, b) -> Double.compare(
                        haversine(lat, lng, a.getLatitude(), a.getLongitude()),
                        haversine(lat, lng, b.getLatitude(), b.getLongitude())
                ))
                .map(Toilet::getName)
                .distinct()
                .limit(3)
                .collect(Collectors.toList());

        return new StationSuggestResponse(names);
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371e3; // meters
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi/2) * Math.sin(dPhi/2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(dLambda/2) * Math.sin(dLambda/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}


