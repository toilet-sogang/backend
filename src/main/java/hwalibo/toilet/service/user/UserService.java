package hwalibo.toilet.service.user;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.request.ReviewPhotoUpdateRequest;
import hwalibo.toilet.dto.review.photo.response.ReviewPhotoUpdateResponse;
import hwalibo.toilet.dto.user.request.UserNameUpdateRequest;
import hwalibo.toilet.dto.user.response.UserResponse;
import hwalibo.toilet.dto.user.response.UserUpdateResponse;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.exception.user.DuplicateUserNameException;
import hwalibo.toilet.exception.user.UserNotFoundException;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.user.UserRepository;
import hwalibo.toilet.service.s3.S3UploadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @PersistenceContext
    private EntityManager entityManager;

    // 로그인된 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponse getUserInfo(User loginUser) {

        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);

        // 전체 유저 수
        long totalUsers = userRepository.count();

        // 나보다 리뷰 수가 많은 유저 수
        long higherRank = userRepository.countByNumReviewGreaterThan(
                user.getNumReview() != null ? user.getNumReview() : 0
        );

        // 상위 퍼센트 (정수)
        int rate = totalUsers > 0
                ? (int) Math.ceil(higherRank * 100.0 / totalUsers)
                : 100;

        return UserResponse.from(user, rate);
    }

    @Transactional
    public ReviewPhotoUpdateResponse updateImage(User loginUser, Long reviewId, ReviewPhotoUpdateRequest request, List<MultipartFile> newImages) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        // 1. (수정) N+1을 피하기 위해 User까지 함께 조회합니다.
        Review review = reviewRepository.findByIdWithUser(reviewId) // (findByIdWithImages 대신 User만 fetch)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        // 2. 권한 검증
        if (!review.getUser().getId().equals(loginUser.getId())) {
            throw new SecurityException("리뷰 수정 권한이 없습니다");
        }

        // 3. (로직 수정) 삭제 로직 - DB에 직접 삭제
        List<Long> deleteIdList = (request != null) ? request.getDeletedImageIds() : null;

        if (deleteIdList != null && !deleteIdList.isEmpty()) {

            // 3-1. DB에서 삭제할 이미지 정보 조회
            List<ReviewImage> imagesToDelete = reviewImageRepository.findAllById(deleteIdList);

            // 3-2. S3 삭제 및 DB 삭제 실행
            for (ReviewImage image : imagesToDelete) {
                // (중요) 이 이미지가 이 리뷰의 것이 맞는지 재확인
                if (image.getReview().getId().equals(reviewId)) {
                    s3UploadService.delete(image.getUrl());
                    log.info("S3에서 이미지 삭제 완료: {}", image.getUrl());
                }
            }
            // 3-3. DB에서 일괄 삭제 (이것이 확실합니다)
            reviewImageRepository.deleteAllInBatch(imagesToDelete);
            log.info("DB에서 이미지 삭제 완료");
        }

        // 4. (로직 수정) 현재 이미지 개수를 DB에서 직접 셉니다. (메모리 의존 X)
        int currentImageCount = reviewImageRepository.countByReviewId(reviewId);
        int newImageCount = (newImages != null) ? newImages.size() : 0;

        if (currentImageCount - (deleteIdList != null ? deleteIdList.size() : 0) + newImageCount > 2) {
            throw new IllegalArgumentException("이미지는 총 2개까지만 등록할 수 있습니다.");
        }

        // 5. (변경 없음) S3에 새 이미지 업로드
        List<String> uploadedUrls = new ArrayList<>();
        if (newImageCount > 0) {
            uploadedUrls = s3UploadService.uploadAll(newImages, "reviews");
        }

        // 6. (로직 수정) DB에서 현재 최대 sortOrder 조회
        int nextOrder = reviewImageRepository.findMaxSortOrderByReviewId(reviewId)
                .orElse(-1) + 1;

        List<ReviewImage> imagesToSave = new ArrayList<>();
        for (String url : uploadedUrls) {
            imagesToSave.add(ReviewImage.builder()
                    .url(url)
                    .review(review) // review 객체는 연관관계 참조용으로만 사용
                    .sortOrder(nextOrder++)
                    .build());
        }

        if (!imagesToSave.isEmpty()) {
            // 7. (변경 없음) 새 이미지들을 DB에 저장
            reviewImageRepository.saveAll(imagesToSave);
            reviewImageRepository.flush();
            log.info("새 이미지 저장 성공");
        }

        // --- ⭐️ 여기가 핵심 ⭐️ ---
        // 8. (추가) 모든 DB 작업이 끝났으므로,
        //      영속성 컨텍스트의 'review' 객체를 버리고,
        //      DB에서 '최신 상태의 리뷰와 이미지 목록'을 다시 조회합니다.

        entityManager.flush();
        entityManager.clear();

        Review refreshedReview = reviewRepository.findByIdWithImages(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        List<String> finalUrls = refreshedReview.getReviewImages().stream()
                .map(ReviewImage::getUrl)
                .collect(Collectors.toList());

        return ReviewPhotoUpdateResponse.of(finalUrls);
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

        // 2. (동일하지 않을 경우에만) 다른 사용자와 중복되는지 검사
        if (userRepository.existsByName(newName)) {
            throw new DuplicateUserNameException("이미 존재하는 닉네임입니다.");
        }

        // 3. 모든 검사를 통과하면 이름 업데이트
        user.updateName(newName);

        return buildUserResponseWithRate(user);
    }


    private UserResponse buildUserResponseWithRate(User user) {
        // 전체 유저 수
        long totalUsers = userRepository.count();

        // 나보다 리뷰 수가 많은 유저 수
        long higherRank = userRepository.countByNumReviewGreaterThan(
                user.getNumReview() != null ? user.getNumReview() : 0
        );

        // 상위 퍼센트 (정수)
        int rate = totalUsers > 0
                ? (int) Math.ceil(higherRank * 100.0 / totalUsers)
                : 100;

        // (이전에 수정한) id가 포함된 UserResponse.from 호출
        return UserResponse.from(user, rate);
    }
}