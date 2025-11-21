package hwalibo.toilet.oauth2.provider;

import hwalibo.toilet.domain.type.Gender;

import java.util.Map;

/**
 * OAuth2 제공자별로 제공하는 사용자 정보의 형식이 다르므로
 * 공통된 형식으로 변환하기 위한 인터페이스
 * 이후에 Google, Facebook, Kakaotalk, Line 등 구현체 추가 가능
 * */
public interface OAuth2UserInfo {

    // 제공자(구글, 네이버 등)의 고유 ID를 반환
    String getProviderId();

    // 제공자 이름 반환
    String getProvider();

    // 사용자 이름(닉네임) 반환
    String getName();

    // 사용자 프로필 이미지 URL 반환
    String getProfileImageUrl();

    //사용자의 성별 정보 반환
    Gender getGender();

    // 사용자 정보가 담긴 원본 Map 데이터 반환
    Map<String, Object> getAttributes();
}