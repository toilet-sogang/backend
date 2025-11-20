package hwalibo.toilet.service.user;

import hwalibo.toilet.respository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRankService {

    private final UserRepository userRepository;

    @Cacheable(value = "userRank", key="#userId.toString()")
    public int calculateUserRate(Long userId) {
        log.info("⚠️ Cache Miss: DB 쿼리 실행. User ID={}", userId);
        return userRepository.findCalculatedRateByUserId(userId)
                .orElse(100);
    }
}