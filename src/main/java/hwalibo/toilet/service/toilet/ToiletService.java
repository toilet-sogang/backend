package hwalibo.toilet.service.toilet;

import com.openai.errors.NotFoundException;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.toilet.response.ToiletDetailResponse;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.exception.toilet.ToiletNotFoundException;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToiletService {
    private final ToiletRepository toiletRepository;

    @Transactional(readOnly = true)
    public ToiletDetailResponse getToiletDetail(User loginUser, Long id){
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        Toilet toilet=toiletRepository.findById(id)
                .orElseThrow(ToiletNotFoundException::new);

        return ToiletDetailResponse.of(toilet);
    }
}
