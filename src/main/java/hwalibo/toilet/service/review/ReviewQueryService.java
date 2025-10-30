package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.response.MyReviewListResponse;
import hwalibo.toilet.dto.review.response.MyReviewResponse;
import hwalibo.toilet.respository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;

    public MyReviewListResponse getMyReviews(User loginUser) {
        List<Review> reviews = reviewRepository.findAllByUser(loginUser);

        // 최신순 정렬: createdAt DESC, updatedAt DESC
        reviews.sort(Comparator
                .comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(Review::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<MyReviewResponse> items = reviews.stream()
                .map(r -> new MyReviewResponse(
                        r.getId(),
                        r.getToilet().getName(),
                        r.getToilet().getGender(),
                        r.getToilet().getNumGate(),
                        r.getDescription(),
                        r.getStar() == null ? null : r.getStar().intValue(),
                        Collections.emptyList(),
                        r.getTag(),
                        r.getCreatedAt(),
                        r.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        return new MyReviewListResponse(items);
    }
}


