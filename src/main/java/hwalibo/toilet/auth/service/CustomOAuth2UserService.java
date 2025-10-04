package hwalibo.toilet.auth.service;

import hwalibo.toilet.auth.CustomOAuth2User;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.domain.type.Role;
import hwalibo.toilet.oauth2.provider.NaverUserInfo;
import hwalibo.toilet.oauth2.provider.OAuth2UserInfo;
import hwalibo.toilet.respository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 기본 OAuth2User 정보 로딩
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // provider별 유저 정보 파싱
        OAuth2UserInfo oAuth2UserInfo;
        if ("naver".equals(registrationId)) {
            oAuth2UserInfo = new NaverUserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        String provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();

        // DB 조회 → 없으면 신규 저장
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> saveNewUser(oAuth2UserInfo));

        // DB에 변경사항 즉시 반영 (refreshToken 같은 필드 업데이트 시 반영 지연 방지)
        userRepository.flush();

        // CustomOAuth2User 반환 (User + attributes)
        return new CustomOAuth2User(user, oAuth2UserInfo.getAttributes());
    }

    private User saveNewUser(OAuth2UserInfo oAuth2UserInfo) {
        User newUser = User.builder()
                .name(oAuth2UserInfo.getName()) // nickname
                .profile(oAuth2UserInfo.getProfileImageUrl())
                .provider(oAuth2UserInfo.getProvider())
                .providerId(oAuth2UserInfo.getProviderId())
                .role(Role.ROLE_USER)
                .build();
        return userRepository.save(newUser);
    }
}

