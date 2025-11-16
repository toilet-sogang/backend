package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.chat.response.ImageStatusResponse;
import hwalibo.toilet.dto.review.photo.response.PhotoUploadResponse;
import hwalibo.toilet.dto.review.request.ReviewCreateRequest;
import hwalibo.toilet.dto.review.response.ReviewCreateResponse;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import hwalibo.toilet.service.review.GoogleVisionValidationService;
import hwalibo.toilet.service.s3.S3UploadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewPostService {
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final S3UploadService s3UploadService;
    private final ToiletRepository toiletRepository;
    private final GoogleVisionValidationService googleVisionValidationService;

    @Transactional
    public ReviewCreateResponse uploadReview(User loginUser, ReviewCreateRequest request, Long toiletId) {

        // 1. 화장실 엔티티 조회 (기존과 동일)
        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화장실입니다."));

        // 2. 리뷰 엔티티 생성 (기존과 동일)
        Review review = request.toEntity(loginUser, toilet);

        // 3. 리뷰 저장 (DB에 우선 저장)
        reviewRepository.save(review);

        // --- 여기부터 갱신 로직 ---

        // 4. (가정) request 객체에서 새 리뷰의 별점을 가져옵니다.
        // 만약 request에 없다면 review.getStar()를 사용합니다.
        // ReviewCreateRequest에 getStar() 메소드가 있다고 가정합니다.
        /*double newReviewStar = request.getStar();

        // 5. 기존 정보 가져오기
        double oldStar = toilet.getStar();
        int oldNumReview = toilet.getNumReview();

        // 6. 새 평균 별점 계산
        // (기존 총 별점 합계 + 새 리뷰 별점) / (새 리뷰 개수)
        double oldTotalStars = oldStar * oldNumReview;
        int newNumReview = oldNumReview + 1;
        double newAverageStar = (oldTotalStars + newReviewStar) / newNumReview;

        // 7. Toilet 엔티티 업데이트 (in-memory)
        toilet.setStar(newAverageStar);
        toilet.setNumReview(newNumReview);*/

        // 8. @Transactional 어노테이션이 있으므로,
        //    메소드가 성공적으로 종료될 때 JPA의 'Dirty Checking' 기능이
        //    변경된 toilet 객체를 감지하고 자동으로 UPDATE 쿼리를 실행해줍니다.
        //    (별도의 toiletRepository.save(toilet) 호출이 필요 없습니다.)

        toilet.updateReviewStats(review.getStar());

        // 9. 응답 반환 (기존과 동일)
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
        //S3저장(동기식)
        uploadedUrls = s3UploadService.uploadAll(images, "reviews");

        int nextOrder = 0;

        List<ReviewImage> newImages = new ArrayList<>();
        //ReviewImage로 변환(PENDING 상태)
        for (String url : uploadedUrls) {
            newImages.add(ReviewImage.builder()
                    .url(url)
                    .sortOrder(nextOrder++)
                    .review(review)
                    .build());
        }

        //db에 이미지 저장
        List<ReviewImage> savedImages = reviewImageRepository.saveAll(newImages);

        //저장된 이미지들을 비동기 검증 서비스에 전달
        for(int i=0;i<savedImages.size();i++){
            ReviewImage savedImage=savedImages.get(i);
            try {
                googleVisionValidationService.validateImage(savedImage.getId(), savedImage.getUrl());
            }catch(Exception e){
                throw new RuntimeException("비동기 호출에서 예외 발생");
            }
        }
        return PhotoUploadResponse.of(savedImages);
    }

    @Transactional(readOnly=true)
    public List<ImageStatusResponse> getImageStatuses(User loginUser, Long reviewId){
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        List<ReviewImage> images = reviewImageRepository.findByReviewId(reviewId);

        return images.stream()
                .map(ImageStatusResponse::new)
                .collect(Collectors.toList());
    }
    }


