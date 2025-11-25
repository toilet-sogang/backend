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
@Table(
        name="users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "providerId"})
        }
)
@SQLDelete(sql = "UPDATE users SET status = 'DELETED', deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "status = 'ACTIVE'")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username; // "provider_providerId" 형식의 고유 식별자

    // 네이버 'nickname'을 저장할 필드
    @Column
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

    //유저의 상태 (회원가입, 회원 탈퇴)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // 탈퇴 일시
    private LocalDateTime deletedAt;

    // Refresh Token
    private String refreshToken;

    // 네이버의 Refresh Token을 저장할 필드
    @Column(length = 1024)
    private String naverRefreshToken;

    // 이름 업데이트
    public void updateName(String newName) {
        this.name = newName;
    }

    //유저의 리뷰 개수 증가
    public void addReview() {
        if(numReview==null) numReview=0;
        this.numReview++;
    }

    //유저의 리뷰 삭제
    public void removeReview() {
        if (numReview == null || numReview <= 0) {
            this.numReview = 0;
            return;
        }
        this.numReview--;
    }

    // 탈퇴 유저 재활성화 메서드
    public void reActivate() {
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
    }

    // 성별 업데이트
    public void updateGender(Gender gender) {
        this.gender = gender;
    }

    // OAuth 프로필 이미지 업데이트 메서드
    public void updateProfileImage(String profileImageUrl) {
        this.profile = profileImageUrl;
    }

    //탈퇴
    public void withdrawAndAnonymize() {
        this.name = "탈퇴한 사용자";
        this.profile = null;
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.refreshToken = null;
        this.naverRefreshToken = null;
    }

    // Refresh Token 업데이트를 위한 메서드
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // 네이버 Refresh Token 업데이트 메서드
    public void updateNaverRefreshToken(String naverRefreshToken) {
        this.naverRefreshToken = naverRefreshToken;
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

    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }
}
