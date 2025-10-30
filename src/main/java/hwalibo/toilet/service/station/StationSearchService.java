package hwalibo.toilet.service.station;

import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.dto.station.response.StationSearchResponse;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StationSearchService {

    private final ToiletRepository toiletRepository;

    public StationSearchService(ToiletRepository toiletRepository) {
        this.toiletRepository = toiletRepository;
    }

    public List<StationSearchResponse> search(String keyword) {
        List<Toilet> toilets = toiletRepository.findByNameContaining(keyword);
        return toilets.stream()
                .map(t -> StationSearchResponse.from(t, t.getStar(), t.getNumReview()))
                .collect(Collectors.toList());
    }
}


