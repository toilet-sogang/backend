package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.type.SortType;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.response.PhotoReviewDetailResponse;
import hwalibo.toilet.dto.review.photo.response.PhotoReviewListResponse;
import hwalibo.toilet.dto.review.response.ReviewListResponse;
import hwalibo.toilet.dto.review.response.ReviewResponse;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import hwalibo.toilet.utils.CursorUtils;
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
    private final ToiletRepository toiletRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;

    @Transactional(readOnly = true)
    public ReviewListResponse getReviewList(User loginUser, Long toiletId, SortType sortType) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화장실입니다."));

        // 같은 성별인지 여부
        boolean canViewPhoto = loginUser.getGender().equals(toilet.getGender());

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
                .map(review -> ReviewResponse.from(review, canViewPhoto))
                .collect(Collectors.toList());

        return new ReviewListResponse(responseList);
    }

    @Transactional(readOnly = true)
    public PhotoReviewListResponse getPhotoReviewList(User loginUser, Long toiletId, String nextCursor, int size) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        Pageable pageable = PageRequest.of(0, size);

        Slice<ReviewImage> imageSlice;
        if (nextCursor == null || nextCursor.isBlank()) {
            //첫 사진 조회
            imageSlice = reviewImageRepository.findFirstPageByToiletId(toiletId, pageable);
        } else {
            try {
                var c = CursorUtils.decode(nextCursor);
                // 이후 사진 조회
                imageSlice = reviewImageRepository.findNextPageByToiletId(toiletId, c.createdAt(), c.id(), pageable);
            } catch (Exception e) {
                // 예: Base64 디코딩 실패 또는 형식 오류
                throw new IllegalArgumentException("잘못된 커서 형식입니다.");
            }
        }


        String newCursor = null;

        if (imageSlice.hasNext()) {
            // getContent()로 실제 List를 가져옵니다.
            List<ReviewImage> content = imageSlice.getContent();

            ReviewImage lastElement = content.get(content.size() - 1); // 마지막 review Image 가져오기

            newCursor = CursorUtils.encode(lastElement.getReview().getCreatedAt(), lastElement.getId());
        }

        return PhotoReviewListResponse.fromReviews(imageSlice, newCursor);
    }

    @Transactional(readOnly = true)
    public PhotoReviewDetailResponse getPhotoReviewDetail(User loginUser, Long toiletId, Long photoId){
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        ReviewImage reviewImage=reviewImageRepository.findByIdWithReviewAndDetails(photoId)
                .orElseThrow(() -> new EntityNotFoundException("사진을 찾을 수 없음"));

        Long actualToiletId = reviewImage.getReview().getToilet().getId();
        if (!actualToiletId.equals(toiletId)) {
            // 사진(105번)은 존재하지만, 요청한 화장실(12번)의 사진이 아님
            throw new IllegalArgumentException("요청한 화장실에 속한 사진이 아닙니다.");
        }

        return PhotoReviewDetailResponse.of(reviewImage.getUrl(),reviewImage.getReview());
    }
}