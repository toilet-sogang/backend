package hwalibo.toilet.auth.handler;

import hwalibo.toilet.auth.CustomOAuth2User;
import hwalibo.toilet.auth.jwt.JwtTokenProvider;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.respository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        // 1. 인증 정보에서 CustomOAuth2User 객체를 가져오기
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser(); // CustomOAuth2User로부터 User 엔티티를 가져오기

        // 2. Access Token과 Refresh Token을 생성
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken();

        log.info("로그인에 성공했습니다. Access Token 발급 완료.");

        // 3. 생성된 Refresh Token을 User 엔티티에 저장
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        log.info("User repository에 Refresh Token 저장 완료");

        // 4. 프론트엔드로 리다이렉트할 URL을 동적으로 생성
        /*String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/auth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();*/

        //개발용: 주석 풀기
        String targetUrl = UriComponentsBuilder.fromPath("/auth/callback.html")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        /*String targetUrl = UriComponentsBuilder.fromUriString("https://hwalibo-backend.duckdns.org/auth/callback.html")  // EC2 IP 반영
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();*/

        log.info("Redirecting to: {}", targetUrl);

        // 5. 생성된 URL로 사용자를 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
