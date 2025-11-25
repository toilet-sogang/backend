package hwalibo.toilet.oauth2.provider;

import hwalibo.toilet.domain.type.Gender;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo {

    // OAuth2User.getAttributes()가 반환하는 Map 데이터를 담을 변수
    private final Map<String, Object> attributes;

    /***
     * 네이버의 경우, 사용자 정보가 'response'라는 키 값 아래에 중첩되어 있음
     * ->생성자에서 'response' 맵을 한 번 더 꺼내줌
     */

    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getName() {
        return (String) attributes.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("profile_image");
    }

    @Override
    public Gender getGender() {
        String naverGenderCode = (String) attributes.get("gender");
        return Gender.fromNaverCode(naverGenderCode);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }
}
