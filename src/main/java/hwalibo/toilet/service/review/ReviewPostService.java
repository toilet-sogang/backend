package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.type.ValidationStatus;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.chat.response.ImageStatusResponse;
import hwalibo.toilet.dto.review.photo.response.PhotoUploadResponse;
import hwalibo.toilet.dto.review.request.ReviewCreateRequest;
import hwalibo.toilet.dto.review.response.ReviewCreateResponse;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import hwalibo.toilet.respository.user.UserRepository;
import hwalibo.toilet.service.review.GoogleVisionValidationService;
import hwalibo.toilet.service.s3.S3UploadService;
import hwalibo.toilet.service.user.UserRankService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewPostService {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final S3UploadService s3UploadService;
    private final ToiletRepository toiletRepository;
    private final GoogleVisionValidationService googleVisionValidationService;
    private final UserRankService userRankService;

    @Transactional
    public ReviewCreateResponse uploadReview(User loginUser, ReviewCreateRequest request, Long toiletId) {

        // 1. 화장실 엔티티 조회 (OK)
        // 'toilet'은 findById로 조회했기 때문에 '영속 상태'입니다.
        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화장실입니다."));

        if(!loginUser.getGender().equals(toilet.getGender())) {
            throw new SecurityException("다른 성별의 화장실 리뷰는 작성할 수 없습니다.");
        }

        // 2. 리뷰 엔티티 생성 (OK)
        // 'review' 엔티티에 'loginUser'를 넣는 것은 FK(user_id)를 설정하기 위함이라 괜찮습니다.
        Review review = request.toEntity(loginUser, toilet);

        // 3. 리뷰 저장 (OK)
        reviewRepository.save(review);

        // 4. ✨ [핵심 수정] DB와 연결된 '영속 상태'의 유저를 다시 불러오기
        User managedUser = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 5. '영속 상태' 유저의 리뷰 개수 최신화
        managedUser.addReview(); // 👈 'loginUser'가 아닌 'managedUser'에 호출해야 합니다.

        // 6. toilet의 Reviewstats 최신화 (OK)
        // 'toilet'도 '영속 상태'이므로 변경 감지(Dirty Checking)가 동작합니다.
        toilet.updateReviewStats(review.getStar());

        userRankService.evictUserRate(loginUser.getId());

        // 7. 응답 반환 (OK)
        return ReviewCreateResponse.of(review);

        // @Transactional이 끝나면, JPA가 'managedUser'와 'toilet'의 변경 사항을
        // 감지하여 자동으로 UPDATE 쿼리를 실행합니다.
    }

    @Transactional
    //이미지 업로드
    public PhotoUploadResponse uploadImage(User loginUser,Long reviewId, List< MultipartFile > images) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        //생성된 리뷰 찾기
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 리뷰입니다."));

        if(!loginUser.getGender().equals( review.getToilet().getGender())) {
            throw new SecurityException("다른 성별의 화장실 리뷰는 작성할 수 없습니다.");
        }

        //사진이 0개인 경우
        if (images == null || images.isEmpty()) return PhotoUploadResponse.of(List.of());


        long currentApprovedCount = review.getReviewImages().stream()
                .filter(image -> image.getStatus() == ValidationStatus.APPROVED)
                .count();

        if (currentApprovedCount + images.size() > 2) {
            throw new IllegalArgumentException("이미지는 총 2개까지만 등록할 수 있습니다.");
        }

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
        // [수정된 부분] 트랜잭션 커밋이 완료된 "후"에 비동기 작업을 실행함
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (ReviewImage savedImage : savedImages) {
                    try {
                        googleVisionValidationService.validateImage(savedImage.getId(), savedImage.getUrl());
                    } catch (Exception e) {
                        log.error("비동기 검수 호출 중 에러: {}", savedImage.getId(), e);
                    }
                }
            }
        });

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


