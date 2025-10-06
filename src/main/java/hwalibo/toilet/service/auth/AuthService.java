package hwalibo.toilet.service.auth;

import hwalibo.toilet.auth.jwt.JwtTokenProvider;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.auth.response.TokenResponse;
import hwalibo.toilet.exception.auth.InvalidTokenException;
import hwalibo.toilet.exception.auth.TokenNotFoundException;
import hwalibo.toilet.exception.auth.UnauthorizedException;
import hwalibo.toilet.respository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
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

    //토큰 재발급
    public TokenResponse reissueTokens(String accessToken, String refreshToken) {

        String keyToCheck = "blacklist:" + accessToken;

        boolean isBlacklisted = (accessToken != null && Boolean.TRUE.equals(redisTemplate.hasKey(keyToCheck)));

        // 1. Access Token이 전달되었고 블랙리스트라면 거부
        if (isBlacklisted) {
            throw new UnauthorizedException("로그아웃된 사용자입니다.");
        }

        // 2. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token 입니다.");
        }

        // 3. DB에서 해당 Refresh Token을 가진 사용자를 찾음
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException("저장소에 Refresh Token이 존재하지 않습니다."));

        // 4. 새로운 토큰들을 생성
        Authentication authentication = jwtTokenProvider.getAuthenticationFromUser(user);
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.createRefreshToken();

        // 5. DB에 새로운 Refresh Token 저장 (Rotation)
        user.updateRefreshToken(newRefreshToken);

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    public void logout(User user, String accessToken) {
        if (user == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        // 1. DB에서 리프레시 토큰 삭제
        user.updateRefreshToken(null);

        // 2. Redis에 블랙리스트 등록
        // JwtTokenProvider가 이미 계산해준 "남은 시간"을 그대로 사용
        long remainingMillis = jwtTokenProvider.getRemainingTime(accessToken);

        // 남은 유효 시간이 존재할 경우에만 블랙리스트에 추가
        if (remainingMillis > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + accessToken,
                    "logout",
                    remainingMillis, // ✅ 이제 올바른 값이 들어갑니다.
                    TimeUnit.MILLISECONDS
            );
            log.info("✅ AccessToken 블랙리스트 등록 완료: {} (유효기간 {}ms)", accessToken, remainingMillis);
        } else {
            log.warn("이미 만료된 AccessToken에 대한 로그아웃 요청입니다: {}", accessToken);
        }
    }
}
