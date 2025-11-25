package hwalibo.toilet.respository.user;

import hwalibo.toilet.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserQueryRepository extends JpaRepository<User, Long> {
    //리뷰 개수 비교
    @Query(value = """
    SELECT calculated_rank.rate
    FROM (
        -- 1. 전체 활성 사용자를 대상으로 순위 계산을 먼저 수행합니다.
        SELECT
            u.id,
            CEIL(
                PERCENT_RANK() OVER (ORDER BY u.num_review DESC) * 100
            ) AS rate
        FROM
            users u
        WHERE
            u.status = 'ACTIVE' 
    ) AS calculated_rank
    WHERE
        calculated_rank.id = :userId
    """, nativeQuery = true)
    Optional<Integer> findCalculatedRateByUserId(@Param("userId") Long userId);

    //유저가 지워졌는지 확인
    @Query(
            value = "SELECT * FROM users WHERE provider = :provider AND provider_id = :providerId LIMIT 1",
            nativeQuery = true
    )
    Optional<User> findUserEvenIfDeleted(@Param("provider") String provider, @Param("providerId") String providerId);
}
