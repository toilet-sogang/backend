package hwalibo.toilet.service.user;

import hwalibo.toilet.respository.user.UserQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRankService {
    private final UserQueryRepository userQueryRepository;

    @Cacheable(value = "userRank", key = "#userId")
    public int calculateUserRate(Long userId) {
        log.info("Cache Miss 발생! DB 조회 - userId={}", userId);
        return userQueryRepository.findCalculatedRateByUserId(userId)
                .orElse(100);
    }

    @CacheEvict(value = "userRank", key = "#userId")
    public void evictUserRate(Long userId) {
        log.info("Rank Cache Evicted! userId={}", userId);
    }
}