package hwalibo.toilet.oauth2.provider;

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
        // 네이버의 고유 ID는 'id' 필드에 있습니다.
        return (String) attributes.get("id");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getName() {
        // 네이버의 닉네임은 'nickname' 필드에 있습니다.
        return (String) attributes.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        // 네이버의 프로필 사진 URL은 'profile_image' 필드에 있습니다.
        return (String) attributes.get("profile_image");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }
}
