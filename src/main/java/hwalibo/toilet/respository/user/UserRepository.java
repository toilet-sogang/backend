package hwalibo.toilet.respository.user;

import hwalibo.toilet.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 토큰 재발급 시 사용
    Optional<User> findByRefreshToken(String refreshToken);

    //닉네임 중복 여부 확인
    boolean existsByName(String name);
}