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
import hwalibo.toilet.service.s3.S3UploadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
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
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;

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
    public UserResponse updateUserName(User loginUser, UserNameUpdateRequest request) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);

        String newName = request.getName();
        String currentName = user.getName();

        // 1. 현재 닉네임과 동일한지 *먼저* 검사
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

        // 1. 리뷰와 이미지를 '함께' 조회합니다. (영속성 컨텍스트에 올림)
        Review review = reviewRepository.findByIdWithImages(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        // 2. 권한 검증
        if (!review.getUser().getId().equals(loginUser.getId())) {
            throw new SecurityException("리뷰 수정 권한이 없습니다");
        }

        // 3. [삭제 로직] (Iterator 사용으로 완벽)
        if (request != null && request.getDeletedImageIds() != null) {

            Set<Long> idsToDelete = new HashSet<>(request.getDeletedImageIds());

            // 3-1. 'review.getReviewImages()' 컬렉션을 직접 순회
            Iterator<ReviewImage> iterator = review.getReviewImages().iterator();

            while (iterator.hasNext()) {
                ReviewImage image = iterator.next();

                if (idsToDelete.contains(image.getId())) {
                    // a. S3에서 삭제
                    s3UploadService.delete(image.getUrl());

                    // b. [핵심] 컬렉션에서 제거 (iterator.remove())
                    // 'orphanRemoval=true'가 이 변경을 감지하고,
                    // 트랜잭션 커밋 시 DB에서 DELETE 쿼리를 실행합니다.
                    iterator.remove();

                    log.info("S3 삭제 및 컬렉션에서 제거 완료:{}", image.getUrl());
                }
            }
        }

        // 4. [추가 로직] (변경 없음)
        int currentImageCount = review.getReviewImages().size(); // (3번에서 삭제된 것이 반영된 정확한 개수)
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
                    .review(review) // 👈 부모(review)와의 연관관계 설정
                    .sortOrder(nextOrder++)
                    .build());
        }

        // 5. [새 이미지 저장] (🚨가장 중요🚨)
        if (!imagesToSave.isEmpty()) {

            // (핵심) 'cascade = CascadeType.ALL'을 믿고 리스트에 더하기만 합니다.
            // JPA가 'review'가 영속 상태인 것을 알고,
            // 트랜잭션 커밋 시 알아서 INSERT 쿼리를 실행합니다.
            review.getReviewImages().addAll(imagesToSave);
            log.info("새 이미지 저장 성공 (컬렉션에 추가 완료)");
        }
        List<String> finalUrls = review.getReviewImages().stream()
                .map(ReviewImage::getUrl).collect(Collectors.toList());

        return ReviewPhotoUpdateResponse.of(finalUrls);
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