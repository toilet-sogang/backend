package hwalibo.toilet.service.review.query;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.response.ImageDto;
import hwalibo.toilet.dto.review.response.user.MyReviewListResponse;
import hwalibo.toilet.dto.review.response.user.MyReviewResponse;
import hwalibo.toilet.respository.review.ReviewQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewQueryRepository reviewQueryRepository;

    public MyReviewListResponse getMyReviews(User loginUser) {
        List<Review> reviews = reviewQueryRepository.findAllByUser(loginUser);
        List<MyReviewResponse> items = reviews.stream()
                .map(r -> new MyReviewResponse(
                        r.getId(),
                        r.getToilet().getName(),
                        r.getToilet().getGender(),
                        r.getToilet().getLine(),
                        r.getDescription(),
                        r.getStar() == null ? null : r.getStar().intValue(),
                        r.getReviewImages().stream()
                                .map(img->new ImageDto(img.getId(),img.getUrl())) // 각 ReviewImage의 id와 url을 받는 image dto 생성
                                .collect(Collectors.toList()),// 반환된 String(URL)들을 새 리스트로 수집,
                        r.getTag(),
                        r.isDis(),
                        r.getCreatedAt(),
                        r.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        return new MyReviewListResponse(items);
    }
}
