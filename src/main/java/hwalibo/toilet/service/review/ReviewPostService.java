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
import java.util.Objects;
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

        // 1. 화장실 엔티티 조회
        Toilet toilet = toiletRepository.findById(toiletId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화장실입니다."));

        // 2. ✨ [핵심 수정] DB에서 최신 유저 정보 조회 (Gender 포함)
        // loginUser의 ID를 사용하여 DB에서 유저를 조회합니다. 이 객체(managedUser)는 정확한 성별 정보를 가집니다.
        User managedUser = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 3. 성별 일치 여부 확인 (managedUser 사용)
        // ⭐️ managedUser의 성별이 NULL이 아니며, 화장실 성별과 다를 경우에만 예외 발생
        if (managedUser.getGender() != null && !Objects.equals(managedUser.getGender(), toilet.getGender())) {
            throw new SecurityException("다른 성별의 화장실 리뷰는 작성할 수 없습니다.");
        }

        // 4. 리뷰 엔티티 생성 시 managedUser 사용
        // FK 설정을 위해 정확한 managedUser 객체를 전달합니다.
        Review review = request.toEntity(managedUser, toilet);

        // 5. 리뷰 저장
        reviewRepository.save(review);

        // 6. '영속 상태' 유저의 리뷰 개수 최신화 (managedUser 사용)
        managedUser.addReview();

        // 7. toilet의 Reviewstats 최신화
        toilet.updateReviewStats(review.getStar());

        userRankService.evictUserRate(loginUser.getId());

        // 8. 응답 반환
        return ReviewCreateResponse.of(review);
    }

    @Transactional
//이미지 업로드
    public PhotoUploadResponse uploadImage(User loginUser,Long reviewId, List< MultipartFile > images) {
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }

        // 1. 생성된 리뷰 찾기 (Toilet 엔티티를 EAGER 로딩하거나 Lazy 로딩되도록 접근)
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 리뷰입니다."));

        // 2. ✨ [핵심 수정] DB에서 최신 유저 정보 조회 (Gender 포함)
        // loginUser의 ID를 사용하여 DB에서 유저를 조회합니다. 이 객체(managedUser)는 정확한 성별 정보를 가집니다.
        User managedUser = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));


        // 3. ✨ 성별 일치 여부 확인 (managedUser 사용)
        // managedUser의 성별이 NULL이 아니며, 리뷰가 달린 화장실 성별과 다를 경우에만 예외 발생
        if (managedUser.getGender() != null && !Objects.equals(managedUser.getGender(), review.getToilet().getGender()))  {
            throw new SecurityException("다른 성별의 화장실에는 이미지를 추가할 수 없습니다.");
        }

        // 4. 사진이 0개인 경우
        if (images == null || images.isEmpty()) return PhotoUploadResponse.of(List.of());


        // 5. 이미지 개수 제한 확인 (기존 로직 유지)
        long currentApprovedCount = review.getReviewImages().stream()
                .filter(image -> image.getStatus() == ValidationStatus.APPROVED)
                .count();

        if (currentApprovedCount + images.size() > 2) {
            throw new IllegalArgumentException("이미지는 총 2개까지만 등록할 수 있습니다.");
        }

        // 6. S3 저장 및 ReviewImage 변환
        List<String> uploadedUrls;
        //S3저장(동기식)
        uploadedUrls = s3UploadService.uploadAll(images, "reviews");

        // Review 엔티티의 reviewImages 컬렉션의 size를 기반으로 nextOrder 설정
        // 기존 approved 이미지 개수를 nextOrder의 시작점으로 사용합니다.
        int nextOrder = (int) currentApprovedCount;

        List<ReviewImage> newImages = new ArrayList<>();
        //ReviewImage로 변환(PENDING 상태)
        for (String url : uploadedUrls) {
            newImages.add(ReviewImage.builder()
                    .url(url)
                    .sortOrder(nextOrder++)
                    .review(review)
                    .build());
        }

        // 7. db에 이미지 저장
        List<ReviewImage> savedImages = reviewImageRepository.saveAll(newImages);

        // 8. 저장된 이미지들을 비동기 검증 서비스에 전달 (트랜잭션 커밋 후 실행)
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


