package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.response.PhotoUploadResponse;
import hwalibo.toilet.dto.review.request.ReviewCreateRequest;
import hwalibo.toilet.dto.review.response.ReviewCreateResponse;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import hwalibo.toilet.service.s3.S3UploadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewPostService {
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final S3UploadService s3UploadService;
    private final ToiletRepository toiletRepository;

    @Transactional
    //리뷰 글 작성
    public ReviewCreateResponse uploadReview(User loginUser, ReviewCreateRequest request,Long toiletId){

        Toilet toilet=toiletRepository.findById(toiletId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화장실입니다."));

        Review review=request.toEntity(loginUser,toilet);

        reviewRepository.save(review);

        double newReviewStar=request.getStar();

        double oldStar=toilet.getStar();
        int oldNumReview=toilet.getNumReview();

        double oldTotalStars=oldStar*oldNumReview;
        int newNumReview=oldNumReview++;
        double newStar=(oldTotalStars+newReviewStar)/newNumReview;

        toilet.updateReviewStats(review.getStar());

        //    @Transactional 어노테이션이 있으므로,
        //    메소드가 성공적으로 종료될 때 JPA의 'Dirty Checking' 기능이
        //    변경된 toilet 객체를 감지하고 자동으로 UPDATE 쿼리를 실행해줍니다.
        //    (별도의 toiletRepository.save(toilet) 호출이 필요 없습니다.)

        return ReviewCreateResponse.of(review);
    }

    @Transactional
    //이미지 업로드
    public PhotoUploadResponse uploadImage(User loginUser,Long reviewId, List< MultipartFile > images) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }
        //사진이 0개인 경우
        if (images == null || images.isEmpty()) return PhotoUploadResponse.of(List.of());
        //생성된 리뷰 찾기
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 리뷰입니다."));
        List<String> uploadedUrls;
        //S3저장
        uploadedUrls = s3UploadService.uploadAll(images, "reviews");

        int nextOrder = 0;

        List<ReviewImage> newImages = new ArrayList<>();
        //ReviewImage로 변환
        for (String url : uploadedUrls) {
            newImages.add(ReviewImage.builder()
                    .url(url)
                    .sortOrder(nextOrder++)
                    .review(review)
                    .build());
        }

        //db에 이미지 저장
        List<ReviewImage> savedImages = reviewImageRepository.saveAll(newImages);

        return PhotoUploadResponse.of(savedImages);
    }
    }


