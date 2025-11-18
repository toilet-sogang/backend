package hwalibo.toilet.respository.user;

import hwalibo.toilet.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 최초 소셜 로그인 시 사용 (회원가입 여부 확인)
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // 토큰 재발급 시 사용
    Optional<User> findByRefreshToken(String refreshToken);

    //리뷰 개수 비교
    long countByNumReviewGreaterThan(Integer numReview);

   //닉네임 중복 여부 확인
    boolean existsByName(String name);

    // @Where(status = 'ACTIVE')를 무시하고 DELETED 상태의 유저도 찾기 위한 쿼리
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId")
    Optional<User> findUserByProviderAndProviderId_IgnoreStatus(@Param("provider") String provider, @Param("providerId") String providerId);
}