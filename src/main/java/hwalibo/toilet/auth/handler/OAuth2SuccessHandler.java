package hwalibo.toilet.auth.handler;

import hwalibo.toilet.auth.CustomOAuth2User;
import hwalibo.toilet.auth.jwt.JwtTokenProvider;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.respository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.oauth2.redirect-uri}")
    private String frontendRedirectUri;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        // 1. 인증 정보에서 CustomOAuth2User 객체를 가져오기
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // 2. Access Token과 Refresh Token을 생성
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String appRefreshToken = jwtTokenProvider.createRefreshToken();

        log.info("로그인에 성공했습니다. Access Token 발급 완료.");

        // 3. oauthToken 캐스팅
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        // 클라이언트 ID ("naver")와 Principal 이름(user.getUsername())을 가져오기
        String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
        String principalName = authentication.getName();

        // authorizedClientService를 사용해 'OAuth2AuthorizedClient'를 로드
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId,
                principalName
        );

        // 로드된 클라이언트에서 네이버 Refresh Token을 추출
        String naverRefreshToken = authorizedClient.getRefreshToken().getTokenValue();

        // 우리 서비스의 Refresh Token과 네이버의 Refresh Token을 User 엔티티에 모두 저장
        user.updateRefreshToken(appRefreshToken);       // 우리 앱 JWT
        user.updateNaverRefreshToken(naverRefreshToken); // 네이버 OAuth2 토큰
        userRepository.save(user);

        log.info("User repository에 (앱/네이버) Refresh Token 저장 완료");

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", appRefreshToken)
                .build().toUriString();

        log.info("Redirecting to: {}", targetUrl);

        // 생성된 URL로 사용자 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}