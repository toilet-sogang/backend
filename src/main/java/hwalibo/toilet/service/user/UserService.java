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
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;

    // 로그인된 유저 정보 조회
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

        Review review=reviewRepository.findByIdWithImages(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        //user가 리뷰 작성자 맞는지 검증
        if(!review.getUser().getId().equals(loginUser.getId())){
            throw new SecurityException("리뷰 수정 권한이 없습니다");
        }

        //삭제할 이미지가 있는 경우
        List<Long>deleteId;
        if(request!=null)deleteId=request.getDeletedImageIds();
        else deleteId=null;

        if(deleteId!=null) {
            List<ReviewImage> imagesToDelete = reviewImageRepository.findAllById(deleteId);

            for (ReviewImage image : imagesToDelete) {
                if (image.getReview().getId().equals(reviewId)) {
                    //s3 삭제
                    s3UploadService.delete(image.getUrl());
                    //repository 삭제
                    reviewImageRepository.delete(image);
                    review.getReviewImages().remove(image);
                    log.info("S3및 DB에서 이미지 삭제 완료:{}", image.getUrl());
                }
            }
        }

            int currentImageCount=review.getReviewImages().size();
            int newImageCount=(newImages!=null)?newImages.size():0;

            //이미지가 2개를 넘는 경우 exception
            if(currentImageCount+newImageCount>2){
                throw new IllegalArgumentException("이미지는 총 2개까지만 등록할 수 있습니다.");
            }

            List<String> uploadedUrls=new ArrayList<>();

            //추가 이미지 s3 등록
            if(newImageCount>0){
                uploadedUrls=s3UploadService.uploadAll(newImages,"reviews");
            }

            int nextOrder=review.getReviewImages().stream()
                    .mapToInt(ReviewImage::getSortOrder).max().orElse(-1)+1;

            //업로드 할 사진들 reviewImage list 생성
            List<ReviewImage> imagesToSave = new ArrayList<>();
            for(String url: uploadedUrls){
                imagesToSave.add(ReviewImage.builder()
                        .url(url)
                        .review(review)
                        .sortOrder(nextOrder++)
                        .build());
            }

            if(!imagesToSave.isEmpty()) {
                //repository 저장 성공
                reviewImageRepository.saveAll(imagesToSave);
                review.getReviewImages().addAll(imagesToSave);
                log.info("새 이미지 저장 성공");
            }

        List<String> finalUrls=review.getReviewImages().stream()
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