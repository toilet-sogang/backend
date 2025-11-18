package hwalibo.toilet.auth.service;

import hwalibo.toilet.auth.CustomOAuth2User;
import hwalibo.toilet.domain.type.UserStatus;
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

        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo oAuth2UserInfo;
        if ("naver".equals(registrationId)) {
            oAuth2UserInfo = new NaverUserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        String provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();

        User user = userRepository.findUserByProviderAndProviderId_IgnoreStatus(provider, providerId)
                .map(existingUser -> {
                    // [ 4. 기존 유저 발견 (ACTIVE 또는 DELETED) ]

                    // (A) 만약 DELETED 상태라면 -> "재활성화" 메서드 호출
                    if (existingUser.getStatus() == UserStatus.DELETED) {
                        // [! 1. 수정 !] setStatus, setDeletedAt 대신 reActivate() 호출
                        existingUser.reActivate();
                    }

                    // (B) 공통: 네이버에서 받은 최신 정보(이름, 프로필)로 업데이트
                    existingUser.updateName(oAuth2UserInfo.getName());
                    // [! 2. 수정 !] setProfile 대신 updateProfileImage() 호출
                    existingUser.updateProfileImage(oAuth2UserInfo.getProfileImageUrl());

                    return userRepository.save(existingUser);
                })
                // [ 5. 신규 유저 ]
                .orElseGet(() -> saveNewUser(oAuth2UserInfo));

        return new CustomOAuth2User(user, oAuth2UserInfo.getAttributes());
    }

    private User saveNewUser(OAuth2UserInfo oAuth2UserInfo) {
        String username = oAuth2UserInfo.getProvider() + "_" + oAuth2UserInfo.getProviderId();
        User newUser = User.builder()
                .username(username)
                .name(oAuth2UserInfo.getName())
                .profile(oAuth2UserInfo.getProfileImageUrl())
                .provider(oAuth2UserInfo.getProvider())
                .providerId(oAuth2UserInfo.getProviderId())
                .role(Role.ROLE_USER)
                // status는 User 엔티티의 @Builder.Default에 의해 자동 설정됨
                .build();
        return userRepository.save(newUser);
    }
}
