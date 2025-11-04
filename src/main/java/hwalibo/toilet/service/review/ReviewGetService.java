package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.type.SortType;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.response.PhotoReviewListResponse;
import hwalibo.toilet.dto.review.response.ReviewListResponse;
import hwalibo.toilet.dto.review.response.ReviewResponse;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class ReviewGetService {
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;

    @Transactional(readOnly = true)
    public ReviewListResponse getReviewList(User loginUser, Long toiletId, SortType sortType) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        List<Review> reviews;
        switch (sortType) {
            case RATING:
                //별점순 정렬
                reviews = reviewRepository.findByToiletId_OrderByRating(toiletId);
                break;
            case HANDICAPPED:
                //장애인 화장실 리뷰만 보기
                reviews = reviewRepository.findByToiletId_HandicappedOnly(toiletId);
            case LATEST:
            default:
                //toiletId로 리뷰 찾기( 최신순 정렬)
                reviews = reviewRepository.findByToiletId_OrderByLatest(toiletId);

        }
        if (reviews.isEmpty()) {
            throw new EntityNotFoundException("해당 화장실에 리뷰가 없습니다.");
        }


        List<ReviewResponse> responseList = reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());

        return new ReviewListResponse(responseList);
    }

    public PhotoReviewListResponse getPhotoReviewList(User loginUser, Long toiletId, Long lastPhotoId, int size) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        Pageable pageable = PageRequest.of(0, size);

        Slice<ReviewImage> imageSlice;
        if (lastPhotoId == null) {
            //첫 사진 조회
            imageSlice = reviewImageRepository.findByToiletIdOrderByIdDesc(toiletId, pageable);
        } else {
            //이후 사진 조회
            imageSlice = reviewImageRepository.findByToiletIdAndIdLessThanOrderByIdDesc(toiletId, lastPhotoId, pageable);
        }
        return PhotoReviewListResponse.fromReviews(imageSlice);

    }
}