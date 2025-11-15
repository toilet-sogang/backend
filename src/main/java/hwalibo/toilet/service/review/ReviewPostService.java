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
    public ReviewCreateResponse uploadReview(User loginUser, ReviewCreateRequest request, Long toiletId) {

        // 1. 화장실 엔티티 조회 (기존과 동일)
        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화장실입니다."));

        // 2. 리뷰 엔티티 생성 (기존과 동일)
        Review review = request.toEntity(loginUser, toilet);

        // 3. 유저의 리뷰 개수 최신화
        loginUser.addReview();

        // 4. 리뷰 저장 (DB에 우선 저장)
        reviewRepository.save(review);

        //5. toilet의 Reviewstats 최신화
        toilet.updateReviewStats(review.getStar());

        // 6. 응답 반환 (기존과 동일)
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


