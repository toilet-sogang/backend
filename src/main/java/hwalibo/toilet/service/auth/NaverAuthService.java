package hwalibo.toilet.service.auth;

import hwalibo.toilet.dto.auth.response.NaverTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverAuthService {

    // Bean으로 등록한 WebClient 주입
    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    // WebClient의 baseUrl 뒤에 붙을 경로
    private static final String NAVER_TOKEN_PATH = "/oauth2.0/token";

    /**
     * 네이버 OAuth2 연동 해제 (토큰 파기)
     * @param naverRefreshToken DB에 저장해둔 유저의 네이버 리프레시 토큰
     */
    public void revokeNaverToken(String naverRefreshToken) {
        if (naverRefreshToken == null) {
            log.warn("Naver Refresh Token이 DB에 없어 연동 해제를 스킵합니다.");
            return;
        }

        try {
            // 1. DB의 Refresh Token으로 Naver의 Access Token을 갱신
            String newAccessToken = refreshNaverAccessToken(naverRefreshToken);

            // 2. 갱신된 Access Token을 사용하여 네이버 연동 해제
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "delete");
            body.add("client_id", naverClientId);
            body.add("client_secret", naverClientSecret);
            body.add("token", newAccessToken);

            String response = webClient.post()
                    .uri(NAVER_TOKEN_PATH)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    // 4xx, 5xx 에러 발생 시 예외로 변환
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("네이버 연동 해제 실패: " + errorBody)))
                    )
                    .bodyToMono(String.class)
                    // (중요) @Transactional 메서드 내에서 호출되므로, 동기식으로 결과를 기다림
                    .block();

            log.info("네이버 연동 해제 성공. 응답: {}", response);

        } catch (Exception e) {
            log.error("네이버 연동 해제 중 예외 발생: {}", e.getMessage(), e);
            // 여기서 에러가 나도 회원 탈퇴(DB 삭제)는 계속 진행되어야 함
        }
    }

    /**
     * 네이버 Access Token 갱신
     */
    private String refreshNaverAccessToken(String naverRefreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("refresh_token", naverRefreshToken);

        // 네이버 API 호출
        NaverTokenResponse response = webClient.post()
                .uri(NAVER_TOKEN_PATH)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                // 에러 처리
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("네이버 Access Token 갱신 실패: " + errorBody)))
                )
                // 1번 DTO로 응답 받기
                .bodyToMono(NaverTokenResponse.class)
                .block(); // 동기식으로 대기

        if (response != null && response.getAccessToken() != null) {
            log.info("네이버 Access Token 갱신 성공.");
            return response.getAccessToken();
        } else {
            throw new RuntimeException("네이버 Access Token 갱신 응답이 비어있습니다.");
        }
    }
}