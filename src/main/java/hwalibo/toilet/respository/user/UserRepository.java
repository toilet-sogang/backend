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
    @Query(value = """
        SELECT
            CEIL(
                (1 - PERCENT_RANK() OVER (ORDER BY u.num_review ASC)) * 100
            ) AS rate
        FROM
            users u
        WHERE
            u.id = :userId
            AND u.status = 'ACTIVE'
    """, nativeQuery = true)
    Optional<Integer> findCalculatedRateByUserId(@Param("userId") Long userId);

    //닉네임 중복 여부 확인
    boolean existsByName(String name);


    @Query(
            value = "SELECT * FROM users WHERE provider = :provider AND provider_id = :providerId LIMIT 1",
            nativeQuery = true
    )
    Optional<User> findUserEvenIfDeleted(@Param("provider") String provider, @Param("providerId") String providerId);

}