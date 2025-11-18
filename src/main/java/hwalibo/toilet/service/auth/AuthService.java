package hwalibo.toilet.service.auth;

import hwalibo.toilet.auth.jwt.JwtTokenProvider;
import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.review.ReviewImage;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.auth.response.TokenResponse;
import hwalibo.toilet.exception.user.UserNotFoundException;
import hwalibo.toilet.exception.auth.InvalidTokenException;
import hwalibo.toilet.exception.auth.TokenNotFoundException;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.respository.review.ReviewImageRepository;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.user.UserRepository;
import hwalibo.toilet.service.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private final S3UploadService s3UploadService;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final NaverAuthService naverAuthService;

    // =================================================================
    // 🔄 토큰 재발급
    // =================================================================
    public TokenResponse reissueTokens(String accessToken, String refreshToken) {

        // 1. Access Token 블랙리스트 확인 (로그아웃된 토큰인지)
        if (accessToken != null && isBlacklisted(accessToken)) {
            throw new UnauthorizedException("로그아웃된 사용자입니다.");
        }

        // 2. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token 입니다.");
        }

        // 3. DB에서 사용자 조회
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException("저장소에 Refresh Token이 존재하지 않습니다."));

        // 4. 새 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(jwtTokenProvider.getAuthenticationFromUser(user));
        String newRefreshToken = jwtTokenProvider.createRefreshToken();

        // 5. DB 업데이트 (Rotation)
        user.updateRefreshToken(newRefreshToken);

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    // =================================================================
    // 🚪 로그아웃
    // =================================================================
    public void logout(User user, String accessToken) {
        if (user == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        // 1. DB에서 Refresh Token 삭제
        user.updateRefreshToken(null);

        // 2. Access Token 블랙리스트 등록 (헬퍼 메서드 사용)
        if (accessToken != null) {
            registerBlacklist(accessToken, "logout");
        }

        log.info("로그아웃 완료: userId={}", user.getId());
    }

    // =================================================================
    // 💀 회원 탈퇴 (옵션 1: 리뷰/이미지 보존 - 현재 사용 중)
    // =================================================================
    public void withdraw(User loginUser, String accessToken) {
        if (loginUser == null) throw new UnauthorizedException("로그인이 필요합니다.");

        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);

        // 1. 네이버 연동 해제
        try {
            naverAuthService.revokeNaverToken(user.getNaverRefreshToken());
        } catch (Exception e) {
            log.error("네이버 연동 해제 실패 (DB 탈퇴는 진행함)", e);
        }

        // 2. 유저 표시 정보 초기화 (익명화)
        user.updateName("탈퇴한 사용자");
        user.updateProfileImage(null);

        // 3. 변경사항 강제 반영 (Update 쿼리 실행)
        userRepository.saveAndFlush(user);

        // 4. Soft delete 수행
        userRepository.delete(user);

        // 5. Access Token 블랙리스트 등록 (즉시 차단)
        if (accessToken != null) {
            registerBlacklist(accessToken, "withdraw-keep-reviews");
        }

        log.info("회원 탈퇴 완료 (리뷰 보존): userId={}", user.getId());
    }

    // =================================================================
    // 💀 회원 탈퇴 (옵션 2: 유저, 리뷰, 이미지 모두 삭제 - 후보)
    // =================================================================
    /*
    public void withdrawDeleteAll(User loginUser, String accessToken) {
        if (loginUser == null) throw new UnauthorizedException("로그인이 필요합니다.");

        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);

        // 1. 유저가 작성한 리뷰 및 이미지 조회
        List<Review> reviews = reviewRepository.findAllByUser(user);

        // 2. S3 삭제 대상 URL 추출
        List<ReviewImage> allImagesToDelete = reviews.stream()
                .flatMap(review -> review.getReviewImages().stream())
                .collect(Collectors.toList());

        List<String> imageUrlsToDelete = allImagesToDelete.stream()
                .map(ReviewImage::getUrl)
                .collect(Collectors.toList());

        // 3. S3 이미지 파일 삭제
        if (!imageUrlsToDelete.isEmpty()) {
            s3UploadService.deleteAll(imageUrlsToDelete);
            log.info("S3 이미지 {}개 삭제 완료", imageUrlsToDelete.size());
        }

        // 4. DB 데이터 삭제 (Cascade 설정이 없다면 순서 중요: 이미지 -> 리뷰)
        if (!allImagesToDelete.isEmpty()) {
            reviewImageRepository.deleteAll(allImagesToDelete);
        }
        if (!reviews.isEmpty()) {
            reviewRepository.deleteAll(reviews);
        }

        // 5. 네이버 연동 해제
        try {
            naverAuthService.revokeNaverToken(user.getNaverRefreshToken());
        } catch (Exception e) {
            log.error("네이버 연동 해제 실패", e);
        }

        // 6. 이름 초기화 (재가입 시 중복 방지)
        user.updateName(null);

        // 7. 유저 Soft Delete
        userRepository.delete(user);

        // 8. Access Token 블랙리스트 등록 (필수!)
        if (accessToken != null) {
            registerBlacklist(accessToken, "withdraw-delete-all");
        }

        log.info("회원 탈퇴 완료 (모든 데이터 삭제): userId={}", user.getId());
    }
    */

    // =================================================================
    // 🛠️ Private Helper Methods (중복 제거 및 로직 캡슐화)
    // =================================================================

    // 블랙리스트 등록 공통 로직
    private void registerBlacklist(String accessToken, String actionType) {
        long remainingMillis = jwtTokenProvider.getRemainingTime(accessToken);
        if (remainingMillis > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + accessToken,
                    actionType,
                    remainingMillis,
                    TimeUnit.MILLISECONDS
            );
            log.info("Access Token 블랙리스트 등록: {} (만료까지 {}ms)", actionType, remainingMillis);
        } else {
            log.debug("만료된 토큰이라 블랙리스트 등록 생략");
        }
    }

    // 블랙리스트 여부 확인 로직
    private boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + accessToken));
    }
}
