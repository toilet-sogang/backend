package hwalibo.toilet.auth.service;

import hwalibo.toilet.auth.CustomOAuth2User;
import hwalibo.toilet.domain.type.Gender;
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
        Gender genderEnum = oAuth2UserInfo.getGender();

        User user = userRepository.findUserEvenIfDeleted(provider, providerId)
                .map(existingUser -> {

                    // 탈퇴 상태면 복구 (재가입)
                    if (existingUser.getStatus() == UserStatus.DELETED) {
                        existingUser.reActivate();
                        existingUser.updateName(oAuth2UserInfo.getName());
                        existingUser.updateProfileImage(oAuth2UserInfo.getProfileImageUrl());
                        existingUser.updateGender(genderEnum);
                    }
                    else {
                        // 이미 ACTIVE 유저
                        existingUser.updateProfileImage(oAuth2UserInfo.getProfileImageUrl());
                        existingUser.updateGender(genderEnum);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> saveNewUser(oAuth2UserInfo)); // 신규 가입자는 그대로 생성
        return new CustomOAuth2User(user, oAuth2UserInfo.getAttributes());
    }

    private User saveNewUser(OAuth2UserInfo oAuth2UserInfo) {
        String username = oAuth2UserInfo.getProvider() + "_" + oAuth2UserInfo.getProviderId();
        Gender genderEnum = oAuth2UserInfo.getGender();

        User newUser = User.builder()
                .username(username)
                .name(oAuth2UserInfo.getName())
                .profile(oAuth2UserInfo.getProfileImageUrl())
                .gender(genderEnum)
                .provider(oAuth2UserInfo.getProvider())
                .providerId(oAuth2UserInfo.getProviderId())
                .role(Role.ROLE_USER)
                .build();
        return userRepository.save(newUser);
    }
}
