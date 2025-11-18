package hwalibo.toilet.domain.user;

import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.Role;
import hwalibo.toilet.domain.type.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
// [1. 추가] delete 쿼리를 이 UPDATE 쿼리로 대체
@SQLDelete(sql = "UPDATE users SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
// [2. 추가] 모든 find 쿼리에 'status = 'ACTIVE'' 조건을 자동 추가
@Where(clause = "status = 'ACTIVE'")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // "provider_providerId" 형식의 고유 식별자

    // 네이버 'nickname'을 저장할 필드
    @Column(unique = true)
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
    @Builder.Default
    private Integer numReview=0; // 리뷰 개수

    // 네이버 'profile_image' URL 하나를 저장할 필드
    private String profile;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // [4. 추가] 탈퇴 일시
    private LocalDateTime deletedAt;

    // [5. 추가] 네이버의 Refresh Token을 저장할 별도 필드
    @Column(length = 1024) // 네이버 리프레시 토큰은 길 수 있으므로 넉넉하게
    private String naverRefreshToken;


    // 이름 업데이트 메서드
    public void updateName(String newName) {
        this.name = newName;
    }

    /**
     * 토큰 갱신을 위한 필드와 메서드들
     * */

    // Refresh Token 저장을 위한 필드 추가
    private String refreshToken;

    // Refresh Token 업데이트를 위한 메서드
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // [6. 추가] 네이버 Refresh Token 업데이트 메서드
    public void updateNaverRefreshToken(String naverRefreshToken) {
        this.naverRefreshToken = naverRefreshToken;
    }

    //유저의 리뷰 개수 추가
    public void addReview() {
        if(numReview==null) numReview=0;
        this.numReview++;
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

    @Override public String getUsername() {return this.username;}

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

    // [7. 수정] UserDetails의 isEnabled()가 status를 반영하도록 수정
    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }
}
