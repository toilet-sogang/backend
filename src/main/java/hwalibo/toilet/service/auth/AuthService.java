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

        // 0. Access Token 블랙리스트 검사
        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + accessToken))) {
            throw new UnauthorizedException("로그아웃된 사용자입니다. 다시 로그인해주세요.");
        }

        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token 입니다.");
        }

        // 2. DB에서 해당 Refresh Token을 가진 사용자를 찾음
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException("저장소에 Refresh Token이 존재하지 않습니다."));

        // 3. 새로운 토큰들을 생성
        Authentication authentication = jwtTokenProvider.getAuthenticationFromUser(user);
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.createRefreshToken();

        // 4. DB에 새로운 Refresh Token 저장 (Rotation)
        user.updateRefreshToken(newRefreshToken);

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(User user,String accessToken) {
        if (user == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        user.updateRefreshToken(null);

        long expiration = jwtTokenProvider.getExpiration(accessToken);
        redisTemplate.opsForValue().set(
                "blacklist:" + accessToken, "logout",
                expiration, TimeUnit.MILLISECONDS
        );
        log.info("✅ AccessToken 블랙리스트 등록 완료: {} (유효기간 {}ms)", accessToken, expiration);
    }

}
