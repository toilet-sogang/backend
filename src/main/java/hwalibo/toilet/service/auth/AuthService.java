package hwalibo.toilet.service.auth;

import hwalibo.toilet.auth.jwt.JwtTokenProvider;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.auth.response.TokenResponse;
import hwalibo.toilet.exception.user.UserNotFoundException;
import hwalibo.toilet.exception.auth.InvalidTokenException;
import hwalibo.toilet.exception.auth.TokenNotFoundException;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.respository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final NaverAuthService naverAuthService;

    //토큰 재발급
    public TokenResponse reissueTokens(String accessToken, String refreshToken) {

        // 1. Access Token 블랙리스트 확인
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

        // 5. DB 업데이트
        user.updateRefreshToken(newRefreshToken);

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    //로그아웃
    public void logout(User user, String accessToken) {
        if (user == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        // 1. DB에서 Refresh Token 삭제
        user.updateRefreshToken(null);

        // 2. Access Token 블랙리스트 등록
        if (accessToken != null) {
            registerBlacklist(accessToken, "logout");
        }
        log.info("로그아웃 완료: userId={}", user.getId());
    }

    //회원 탈퇴 (soft)
    public void withdraw(User loginUser, String accessToken) {
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);

        // 1. 네이버 연동 해제
        try {
            naverAuthService.revokeNaverToken(user.getNaverRefreshToken());
        } catch (Exception e) {
            log.error("네이버 연동 해제 실패 (DB 탈퇴는 진행함)", e);
        }

        // 2. 익명화 및 Soft delete를 엔티티 내에서 한번에 처리
        user.withdrawAndAnonymize();

        // 3. Access Token 블랙리스트 등록
        if (accessToken != null) {
            registerBlacklist(accessToken, "withdraw-keep-reviews");
        }

        log.info("회원 탈퇴 완료 (리뷰 보존): userId={}", user.getId());
    }

    //Helper Method

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
