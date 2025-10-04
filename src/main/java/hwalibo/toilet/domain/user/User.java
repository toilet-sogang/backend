package hwalibo.toilet.domain.user;

import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 네이버 'nickname'을 저장할 필드
    private String name;

    // 네이버 'gender'를 저장할 필드
    @Enumerated(EnumType.STRING)
    private Gender gender;

    // OAuth2 제공자 (e.g., "naver", "google")
    private String provider;

    // OAuth2 제공자의 고유 ID
    private String providerId;

    @Enumerated(EnumType.STRING)
    private Role role; // 사용자 권한 (ROLE_USER, ROLE_ADMIN)

    @Column(columnDefinition = "DOUBLE DEFAULT 0.0")
    private double rate; // 사용자 순위

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer numReview; // 리뷰 개수

    // 네이버 'profile_image' URL 하나를 저장할 필드
    private String profile;

    /**
     * 토큰 갱신을 위한 필드와 메서드들
     * */

    // Refresh Token 저장을 위한 필드 추가
    private String refreshToken;

    // Refresh Token 업데이트를 위한 메서드
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // 이름 업데이트 메서드
    public void updateName(String newName) {
        this.name = newName;
    }


    // ========= UserDetails 인터페이스 구현  ========== //

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return provider + "_" + providerId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
