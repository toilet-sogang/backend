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

    // [! 2. 의존성 주입 추가 !]
    // (이 의존성을 추가하기 위해 @RequiredArgsConstructor가 클래스에 있는지 확인하세요)
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.oauth2.redirect-uri}")
    private String frontendRedirectUri;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        // 1. 인증 정보에서 CustomOAuth2User 객체를 가져오기 (변경 없음)
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // 2. (우리 앱의) Access Token과 Refresh Token을 생성 (변경 없음)
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String appRefreshToken = jwtTokenProvider.createRefreshToken();

        log.info("로그인에 성공했습니다. Access Token 발급 완료.");

        // [! 3. ★★★ 핵심 수정 (에러 해결) ★★★ !]
        // (에러가 났던) OAuth2LoginAuthenticationToken으로 캐스팅하는 대신,
        // OAuth2AuthenticationToken (부모 클래스)으로 캐스팅합니다. (이건 안전함)
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        // 클라이언트 ID ("naver")와 Principal 이름(user.getUsername())을 가져옵니다.
        String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
        String principalName = authentication.getName(); // (DB에 저장된 user.getUsername()과 동일)

        // authorizedClientService를 사용해 'OAuth2AuthorizedClient'를 로드합니다.
        // 이 객체 안에 Refresh Token이 들어있습니다.
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId,
                principalName
        );

        // 로드된 클라이언트에서 '진짜' 네이버 Refresh Token을 추출합니다.
        String naverRefreshToken = authorizedClient.getRefreshToken().getTokenValue();

        // (우리 앱의) Refresh Token과 (네이버의) Refresh Token을 User 엔티티에 모두 저장
        user.updateRefreshToken(appRefreshToken);       // 우리 앱 JWT
        user.updateNaverRefreshToken(naverRefreshToken); // 네이버 OAuth2 토큰
        userRepository.save(user);

        log.info("User repository에 (앱/네이버) Refresh Token 저장 완료");
        // [! 3. ★★★ 수정 끝 ★★★ !]


        // [! 4. ★★★ 리디렉션 URI 수정 ★★★ !]
        // 프론트엔드 URI의 주석을 해제합니다.
        /*String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", appRefreshToken) // 우리 앱 토큰 전달
                .build().toUriString();*/

        //개발용: '/auth/callback.html' 경로는 주석 처리합니다.

        String targetUrl = UriComponentsBuilder.fromPath("/auth/callback.html")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", appRefreshToken)
                .build().toUriString();

        // [! 4. ★★★ 수정 끝 ★★★ !]

        log.info("Redirecting to: {}", targetUrl);

        // 5. 생성된 URL로 사용자를 리다이렉트 (변경 없음)
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}