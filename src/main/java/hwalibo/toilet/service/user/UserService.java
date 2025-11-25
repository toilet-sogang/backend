package hwalibo.toilet.service.user;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.type.ValidationStatus;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.request.ReviewPhotoUpdateRequest;
import hwalibo.toilet.dto.review.photo.response.ReviewPhotoUpdateResponse;
import hwalibo.toilet.dto.user.request.UserNameUpdateRequest;
import hwalibo.toilet.dto.user.response.UserResponse;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.exception.user.DuplicateUserNameException;
import hwalibo.toilet.exception.user.IdenticalNameException;
import hwalibo.toilet.exception.user.UserNotFoundException;
import hwalibo.toilet.respository.review.image.ReviewImageQueryRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.user.UserRepository;
import hwalibo.toilet.service.review.googlevision.GoogleVisionValidationService;
import hwalibo.toilet.service.s3.S3UploadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final ReviewRepository reviewRepository;
    private final ReviewImageQueryRepository reviewImageQueryRepository;
    private final GoogleVisionValidationService googleVisionValidationService;
    private final UserRankService userRankService;

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

    //유저 이름 수정
    @Transactional
    public UserResponse updateUserName(User loginUser, UserNameUpdateRequest request) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);
        String newName = request.getName();
        String currentName = user.getName();
        if (newName.equals(currentName)) {
            throw new IdenticalNameException("현재 닉네임과 동일한 닉네임입니다.");
        }
        if (userRepository.existsByName(newName)) {
            throw new DuplicateUserNameException("이미 존재하는 닉네임입니다.");
        }
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

        //rejected 이미지는 삭제
        Iterator<ReviewImage> rejectIterator = review.getReviewImages().iterator();
        while (rejectIterator.hasNext()) {
            ReviewImage image = rejectIterator.next();
            if (image.getStatus() == ValidationStatus.REJECTED) {
                s3UploadService.delete(image.getUrl());
                rejectIterator.remove();
            }
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
        long currentImageCount = review.getReviewImages().stream()
                .filter(image->image.getStatus()== ValidationStatus.APPROVED).count();
        int newImageCount = (newImages != null) ? newImages.size() : 0;

        if (currentImageCount + newImageCount > 2) {
            throw new IllegalArgumentException("이미지는 총 2개까지만 등록할 수 있습니다.");
        }

        List<NewImageContext> contexts = new ArrayList<>();
        if (newImageCount > 0) {
            List<String> uploadedUrls = s3UploadService.uploadAll(newImages, "reviews");

            int nextOrder = review.getReviewImages().stream()
                    .mapToInt(ReviewImage::getSortOrder).max().orElse(-1) + 1;

            for (int i = 0; i < uploadedUrls.size(); i++) {
                String url = uploadedUrls.get(i);

                ReviewImage image = ReviewImage.builder()
                        .url(url)
                        .review(review)
                        .sortOrder(nextOrder++)
                        .status(ValidationStatus.PENDING) // 일단 PENDING (비동기 검수 전)
                        .build();

                review.getReviewImages().add(image);

                contexts.add(new NewImageContext(i, image));
                reviewRepository.flush();
            }
        }

        if (!contexts.isEmpty()) {
            //이미지 비동기 검수
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (NewImageContext ctx : contexts) {
                        try {
                            if (ctx.getImage().getId() != null) {
                                googleVisionValidationService.validateImage(ctx.getImage().getId(),ctx.getImage().getUrl());
                            }
                        } catch (Exception e) {
                            log.error("이미지 수정 비동기 검수 호출 실패: index={}, url={}", ctx.getIndexInRequest(), ctx.getImage().getUrl());
                        }
                    }
                }
            });


            log.info("새 이미지 저장 예약 및 비동기 검수 등록 완료");
        }

        List<ReviewPhotoUpdateResponse.UpdatedPhotoDto> dtos = contexts.stream()
                .map(ctx -> ReviewPhotoUpdateResponse.UpdatedPhotoDto.builder()
                        .index(ctx.getIndexInRequest())
                        .imageId(ctx.getImage().getId())
                        .build())
                .toList();

        return ReviewPhotoUpdateResponse.of(dtos);

    }

    private UserResponse buildUserResponseWithRate(User user) {
        int rate = userRankService.calculateUserRate(user.getId());
        return UserResponse.from(user, rate);
    }

    @Getter
    @AllArgsConstructor
    private static class NewImageContext {
        private int indexInRequest; // 프론트가 보낸 photos 배열의 인덱스 (0, 1, 2...)
        private ReviewImage image;  // DB에 저장된(혹은 저장할) 엔티티
    }
}