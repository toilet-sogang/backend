package hwalibo.toilet.init;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.type.Role;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import hwalibo.toilet.respository.user.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("ToiletDataLoader")
//@Profile({"local", "dev"}) // 로컬/개발 환경에서만 실행하려면 주석 해제
public class DummySeedReviewer {

    private final UserRepository userRepository;
    private final ToiletRepository toiletRepository;
    private final ReviewRepository reviewRepository;

    // 필요 시 대상 화장실 ID 변경
    private static final long TARGET_TOILET_ID = 1L;

    @PostConstruct
    @Transactional
    public void seed() {
        // 대상 화장실 확인
        Optional<Toilet> toiletOpt = toiletRepository.findById(TARGET_TOILET_ID);
        if (toiletOpt.isEmpty()) {
            log.warn("⚠️ 대상 화장실(id={})이 존재하지 않아 더미 리뷰 시드를 건너뜁니다.", TARGET_TOILET_ID);
            return;
        }
        Toilet toilet = toiletOpt.get();

        // 이미 해당 화장실에 리뷰가 있으면 skip
        long existing = reviewRepository.countByToiletId(TARGET_TOILET_ID);
        if (existing > 0) {
            log.info("✅ 화장실(id={})에 {}개의 리뷰가 이미 존재합니다. 더미 시드 스킵.", TARGET_TOILET_ID, existing);
            return;
        }

        // 더미 유저 5명 확보/생성 (provider='seed' 네임스페이스)
        List<User> seeds = ensureSeedUsers();

        // 리뷰 10건 생성
        List<Review> reviews = new ArrayList<>();
        reviews.add(newReview(toilet, seeds.get(0), "출구와 가까워 찾기 쉽고 전반적으로 깨끗했습니다. 휴지와 손세정제가 넉넉했어요.", 4.5, 3, false));
        reviews.add(newReview(toilet, seeds.get(1), "냄새가 거의 없고 청소 주기가 짧은 듯해요. 세면대 물 잘 나옵니다.", 4.2, 1, false));
        reviews.add(newReview(toilet, seeds.get(2), "출근 시간대엔 조금 붐빕니다. 그래도 칸 수가 많아 대기는 짧았어요.", 3.8, 0, false));
        reviews.add(newReview(toilet, seeds.get(3), "조명이 밝고 거울이 깨끗해요. 휴지통 비움만 조금 더 자주 하면 좋겠어요.", 4.0, 0, false));
        reviews.add(newReview(toilet, seeds.get(4), "바닥에 물기가 조금 있어 미끄러울 수 있어요. 환기는 잘 되는 편.", 3.4, 0, false));
        reviews.add(newReview(toilet, seeds.get(0), "세정제 구비, 물 내림 이상 없음. 전반적으로 쾌적했습니다.", 4.3, 2, false));
        reviews.add(newReview(toilet, seeds.get(1), "안내 표지 명확해서 금방 찾았고, 칸 내부도 깔끔했습니다.", 4.1, 0, false));
        reviews.add(newReview(toilet, seeds.get(2), "소음이 조금 있었지만 냄새가 거의 없어 무난했어요.", 3.9, 0, false));
        reviews.add(newReview(toilet, seeds.get(3), "휴지통이 꽉 차 있었던 점만 아쉬웠고, 나머지는 만족스러웠습니다.", 3.7, 0, false));
        reviews.add(newReview(toilet, seeds.get(4), "세면대 주변 정리 상태 좋고, 전반적인 청결도가 우수했습니다.", 4.4, 1, false));

        reviewRepository.saveAll(reviews);
        log.info("✅ 더미 리뷰 {}건을 화장실(id={})에 삽입 완료.", reviews.size(), TARGET_TOILET_ID);
    }

    private List<User> ensureSeedUsers() {
        String provider = "seed";
        String[] ids = {"u1", "u2", "u3", "u4", "u5"};
        String[] names = {"seed_alice", "seed_bob", "seed_cindy", "seed_david", "seed_eric"};

        List<User> result = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            String pid = ids[i];
            String name = names[i];

            User user = userRepository
                    .findByProviderAndProviderId(provider, pid)
                    .orElseGet(() -> userRepository.save(
                            User.builder()
                                    .name(name)
                                    .provider(provider)
                                    .providerId(pid)
                                    .role(Role.ROLE_USER)
                                    .rate(0.0)
                                    .numReview(0)
                                    .profile(null)
                                    .refreshToken(null)
                                    .build()
                    ));
            result.add(user);
        }
        return result;
    }

    private Review newReview(Toilet toilet, User user, String desc, double star, int good, boolean isDis) {
        return Review.builder()
                .toilet(toilet)
                .user(user)
                .description(desc)
                .star(star)
                .photo(new ArrayList<>()) // 사진 제외
                .good(good)
                .tag(new ArrayList<>())   // 태그 제외
                .isDis(isDis)
                .build();
        // @CreatedDate / @LastModifiedDate는 Auditing으로 자동 설정됨 (@EnableJpaAuditing 필요)
    }
}
