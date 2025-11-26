package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.SortType;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.response.PhotoReviewDetailResponse;
import hwalibo.toilet.dto.review.photo.response.PhotoReviewListResponse;
import hwalibo.toilet.dto.review.response.ReviewListResponse;
import hwalibo.toilet.dto.review.response.ReviewResponse;
import hwalibo.toilet.respository.review.ReviewQueryRepository;
import hwalibo.toilet.respository.review.image.ReviewImageQueryRepository;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import hwalibo.toilet.utils.CursorUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class ReviewGetService {
    private final ToiletRepository toiletRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final ReviewImageQueryRepository reviewImageQueryRepository;


    @Transactional(readOnly = true)
    public ReviewListResponse getReviewList(User loginUser, Long toiletId, SortType sortType) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화장실입니다."));

        // 같은 성별인지 여부
        boolean canViewPhoto = Objects.equals(loginUser.getGender(), toilet.getGender());

        List<Review> reviews;
        switch (sortType) {
            case RATING:
                //별점순 정렬
                reviews = reviewQueryRepository.findByToiletId_OrderByRating(toiletId);
                break;
            case HANDICAPPED:
                //장애인 화장실 리뷰만 보기
                reviews = reviewQueryRepository.findByToiletId_HandicappedOnly(toiletId);
            case LATEST:
            default:
                //toiletId로 리뷰 찾기( 최신순 정렬)
                reviews = reviewQueryRepository.findByToiletId_OrderByLatest(toiletId);

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

        // 로그인 유저의 성별을 가져옵니다.
        Gender userGender = loginUser.getGender();

        Pageable pageable = PageRequest.of(0, size);

        Slice<ReviewImage> imageSlice;
        if (nextCursor == null || nextCursor.isBlank()) {
            //첫 사진 조회
            //userGender를 두 번째 파라미터로 전달
            imageSlice = reviewImageQueryRepository.findFirstPageByToiletId(toiletId, userGender, pageable);
        } else {
            try {
                var c = CursorUtils.decode(nextCursor);
                // 이후 사진 조회
                // userGender를 두 번째 파라미터로 전달
                imageSlice = reviewImageQueryRepository.findNextPageByToiletId(toiletId, userGender, c.createdAt(), c.id(), pageable);
            } catch (Exception e) {
                // 예: Base64 디코딩 실패 또는 형식 오류
                throw new IllegalArgumentException("잘못된 커서 형식입니다.");
            }
        }


        String newCursor = null;

        if (imageSlice.hasNext()) {
            // getContent()로 실제 List를 가져오기
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

        ReviewImage reviewImage=reviewImageQueryRepository.findByIdWithReviewAndDetails(photoId)
                .orElseThrow(() -> new EntityNotFoundException("사진을 찾을 수 없음"));

        Long actualToiletId = reviewImage.getReview().getToilet().getId();
        if (!actualToiletId.equals(toiletId)) {
            // 사진은 존재하지만, 요청한 화장실의 사진이 아님
            throw new IllegalArgumentException("요청한 화장실에 속한 사진이 아닙니다.");
        }

        // 성별 필터링 로직 추가
        Gender userGender = loginUser.getGender();
        Gender toiletGender = reviewImage.getReview().getToilet().getGender();

        if (!Objects.equals(userGender, toiletGender)) {
            // 성별이 다르면 접근 거부
            throw new SecurityException("접근 권한이 없습니다. 해당 성별의 리뷰 사진이 아닙니다.");
        }

        return PhotoReviewDetailResponse.of(reviewImage.getUrl(),reviewImage.getReview());
    }
}