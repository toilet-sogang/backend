package hwalibo.toilet.service.user;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.request.ReviewPhotoUpdateRequest;
import hwalibo.toilet.dto.review.photo.response.ReviewPhotoUpdateResponse;
import hwalibo.toilet.dto.user.request.UserNameUpdateRequest;
import hwalibo.toilet.dto.user.response.UserResponse;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.exception.user.DuplicateUserNameException;
import hwalibo.toilet.exception.user.IdenticalNameException;
import hwalibo.toilet.exception.user.UserNotFoundException;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.user.UserRepository;
import hwalibo.toilet.service.review.GoogleVisionValidationService;
import hwalibo.toilet.service.s3.S3UploadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final GoogleVisionValidationService googleVisionValidationService;

    // 로그인된 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponse getUserInfo(User loginUser) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);
        return buildUserResponseWithRate(user);
    }

    @Transactional
    public UserResponse updateUserName(User loginUser, UserNameUpdateRequest request) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);
        String newName = request.getName();
        String currentName = user.getName();
        // 1. 현재 닉네임과 동일한지 검사
        if (newName.equals(currentName)) {
            throw new IdenticalNameException("현재 닉네임과 동일한 닉네임입니다.");
        }
        // 2. (동일하지 않을 경우에만) 다른 사용자와 중복되는지 검사
        if (userRepository.existsByName(newName)) {
            throw new DuplicateUserNameException("이미 존재하는 닉네임입니다.");
        }
        // 3. 모든 검사를 통과하면 이름 업데이트
        user.updateName(newName);
        return buildUserResponseWithRate(user);
    }

    @Transactional
    public ReviewPhotoUpdateResponse updateImage(User loginUser, Long reviewId, ReviewPhotoUpdateRequest request, List<MultipartFile> newImages) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        // 1. (🚨핵심 수정🚨) 'JOIN FETCH' 쿼리 대신, '부모' 엔티티만 조회합니다.
        // 'reviewImages' 리스트는 아직 로드되지 않은 'Lazy Loading' 상태입니다.
        Review review = reviewRepository.findById(reviewId) // 👈 'WithImages'가 빠졌습니다.
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        // 2. (변경 없음) 권한 검증
        if (!review.getUser().getId().equals(loginUser.getId())) {
            throw new SecurityException("리뷰 수정 권한이 없습니다");
        }

        // 3. [삭제 로직]
        if (request != null && request.getDeletedImageIds() != null) {
            Set<Long> idsToDelete = new HashSet<>(request.getDeletedImageIds());

            // 3-1. (⭐️중요⭐️) 'review.getReviewImages()'에 *처음 접근*하는 순간,
            // JPA가 "아, 이제 자식 리스트가 필요하구나"라고 인지하고
            // Lazy Loading으로 '수정 가능한' 리스트를 DB에서 SELECT 해옵니다.
            Iterator<ReviewImage> iterator = review.getReviewImages().iterator();

            while (iterator.hasNext()) {
                ReviewImage image = iterator.next();

                if (idsToDelete.contains(image.getId())) {
                    // a. S3에서 삭제
                    s3UploadService.delete(image.getUrl());

                    // b. '수정 가능한' 리스트에서 삭제 (정상 동작)
                    // 'orphanRemoval=true'가 100% 인지하고 DB에 'DELETE'를 예약합니다.
                    iterator.remove();

                    log.info("S3 삭제 및 컬렉션에서 제거 완료:{}", image.getUrl());
                }
            }
        }

        // 4. [추가 로직] (변경 없음)
        int currentImageCount = review.getReviewImages().size();
        int newImageCount = (newImages != null) ? newImages.size() : 0;

        if (currentImageCount + newImageCount > 2) {
            throw new IllegalArgumentException("이미지는 총 2개까지만 등록할 수 있습니다.");
        }

        List<String> uploadedUrls = new ArrayList<>();
        if (newImageCount > 0) {
            uploadedUrls = s3UploadService.uploadAll(newImages, "reviews");
        }

        int nextOrder = review.getReviewImages().stream()
                .mapToInt(ReviewImage::getSortOrder).max().orElse(-1) + 1;

        List<ReviewImage> imagesToSave = new ArrayList<>();
        for (String url : uploadedUrls) {
            imagesToSave.add(ReviewImage.builder()
                    .url(url)
                    .review(review) // 부모(review)와의 연관관계 설정
                    .sortOrder(nextOrder++)
                    .build());
        }

        if (!imagesToSave.isEmpty()) {

            // 5-1. 컬렉션에 추가 (Cascade 저장 예약)
            review.getReviewImages().addAll(imagesToSave);

            // 5-2. ✨ [추가된 부분] 트랜잭션 커밋 후 비동기 검수 실행
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 커밋이 완료되면 imagesToSave의 객체들에 ID가 생성되어 있습니다.
                    for (ReviewImage image : imagesToSave) {
                        try {
                            if (image.getId() != null) {
                                googleVisionValidationService.validateImage(image.getId(), image.getUrl());
                            }
                        } catch (Exception e) {
                            log.error("이미지 수정 비동기 검수 호출 실패: ID={}, URL={}", image.getId(), image.getUrl(), e);
                        }
                    }
                }
            });

            log.info("새 이미지 저장 예약 및 비동기 검수 등록 완료");
        }

        Review updatedReview = reviewRepository.findByIdWithImages(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다. ID: " + reviewId));


        List<ReviewImage> finalImages = updatedReview.getReviewImages();

        return ReviewPhotoUpdateResponse.of(finalImages);

    }

    @Cacheable(value = "userRank", key = "#userId")
    private int calculateUserRate(Long userId) {
        log.info("⚠️ Cache Miss: 순위 계산을 위해 DB 쿼리 실행. User ID: {}", userId);

        // findCalculatedRateByUserId의 쿼리도 userId를 받도록 이미 되어 있음
        int rate = userRepository.findCalculatedRateByUserId(userId)
                .orElse(100);
        return rate;
    }

    private UserResponse buildUserResponseWithRate(User user) {
        int rate = calculateUserRate(user.getId());
        return UserResponse.from(user, rate);
    }
}