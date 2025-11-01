/*package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.response.ReviewListResponse;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.respository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private ReviewRepository reviewRepository;

    @Transactional(readOnly=true)
    public ReviewListResponse reviewList(User loginUser,Long id) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        Review review=reviewRepository

    }

}*/
