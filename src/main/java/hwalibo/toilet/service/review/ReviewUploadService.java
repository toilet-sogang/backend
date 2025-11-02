package hwalibo.toilet.service.review;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.review.photo.response.PhotoUploadResponse;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.service.s3.S3UploadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewUploadService {
    private ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final S3UploadService s3UploadService;


    @Transactional
    public PhotoUploadResponse uploadImage(User loginUser,Long reviewId, List< MultipartFile > images){
        if (loginUser == null) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }
        //사진이 0개인 경우
        if (images == null || images.isEmpty()) return PhotoUploadResponse.of(List.of());
        //생성된 리뷰 찾기
        Review review=reviewRepository.findById(reviewId)
                    .orElseThrow(()->new EntityNotFoundException("존재하지 않는 리뷰입니다."));
        List<String> uploadedUrls;
        //S3저장
        uploadedUrls= s3UploadService.uploadAll(images,"reviews");

        int nextOrder=0;

        for(String url:uploadedUrls){
            ReviewImage newImage=ReviewImage.builder()
                    .url(url)
                    .sortOrder(nextOrder++)
                    .review(review)
                    .build();
            reviewImageRepository.save(newImage);
        }

        return PhotoUploadResponse.of(uploadedUrls);
    }

}
