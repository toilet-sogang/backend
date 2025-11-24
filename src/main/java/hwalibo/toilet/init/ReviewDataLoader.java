package hwalibo.toilet.init;

import hwalibo.toilet.domain.review.Review;
import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.Tag;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.respository.review.ReviewRepository;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import hwalibo.toilet.respository.user.UserRepository;
import hwalibo.toilet.service.user.UserRankService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("ToiletDataLoader")
public class ReviewDataLoader {

    private final ReviewRepository reviewRepository;
    private final ToiletRepository toiletRepository;
    private final UserRepository userRepository;
    private final UserRankService userRankService;

    private static final List<Long> USER_IDS = Arrays.asList(1L,2L,3L,4L,5L,6L,7L);
    private final Random random = new Random();

    // 긍정 태그 / 부정 태그
    private static final List<Tag> POSITIVE_TAGS = Arrays.asList(
            Tag.TOILET_CLEAN, Tag.SINK_CLEAN, Tag.GOOD_VENTILATION,
            Tag.ENOUGH_HANDSOAP, Tag.BRIGHT_LIGHTING
    );

    private static final List<Tag> NEGATIVE_TAGS = Arrays.asList(
            Tag.TRASH_OVERFLOW, Tag.DIRTY_FLOOR, Tag.DIRTY_MIRROR,
            Tag.NO_TOILET_PAPER, Tag.BAD_ODOR
    );

    private record ReviewTemplate(String description, Tag mainTag, int minStar, int maxStar) {}

    // ★ 리뷰 템플릿 (그대로 유지)
    private static final List<ReviewTemplate> TEMPLATES = List.of(
            // 긍정 리뷰 15개
            new ReviewTemplate("변기 위생 상태가 매우 훌륭합니다. [STATION]역 최고! (평점: %d점)", Tag.TOILET_CLEAN,4,5),
            new ReviewTemplate("세면대가 물기 없이 깨끗해서 좋았어요. (평점: %d점)", Tag.SINK_CLEAN,4,5),
            new ReviewTemplate("냄새 없이 쾌적했습니다. 환기가 잘 돼요. (평점: %d점)", Tag.GOOD_VENTILATION,4,5),
            new ReviewTemplate("조명이 밝아서 이용하기 편했습니다. (평점: %d점)", Tag.BRIGHT_LIGHTING,4,5),
            new ReviewTemplate("손 세정제가 넉넉했고 휴지도 충분했습니다. (평점: %d점)", Tag.ENOUGH_HANDSOAP,4,5),
            new ReviewTemplate("깨끗하고 넓었습니다. (평점: %d점)", Tag.TOILET_CLEAN,4,5),
            new ReviewTemplate("화장실이 쾌적하고 냄새가 없어요. (평점: %d점)", Tag.GOOD_VENTILATION,5,5),
            new ReviewTemplate("세면대 관리가 잘 되어있어요. (평점: %d점)", Tag.SINK_CLEAN,4,5),
            new ReviewTemplate("밝고 깔끔해요. (평점: %d점)", Tag.BRIGHT_LIGHTING,4,5),
            new ReviewTemplate("이용객이 많아도 청결 상태가 좋아요. (평점: %d점)", Tag.TOILET_CLEAN,5,5),
            new ReviewTemplate("칸막이 공간이 넓어 좋아요. (평점: %d점)", Tag.TOILET_CLEAN,4,5),
            new ReviewTemplate("휴지통이 비워져 있어 깔끔했습니다. (평점: %d점)", Tag.TOILET_CLEAN,5,5),
            new ReviewTemplate("따뜻한 물이 잘 나와요. (평점: %d점)", Tag.SINK_CLEAN,4,5),
            new ReviewTemplate("악취가 거의 없어요. (평점: %d점)", Tag.GOOD_VENTILATION,4,5),
            new ReviewTemplate("센서 등이 잘 작동해요. (평점: %d점)", Tag.BRIGHT_LIGHTING,5,5),

            // 보통 리뷰 15개
            new ReviewTemplate("평범한 화장실입니다. (평점: %d점)", Tag.ENOUGH_HANDSOAP,3,3),
            new ReviewTemplate("약간 꿉꿉한 냄새가 납니다. (평점: %d점)", Tag.GOOD_VENTILATION,3,3),
            new ReviewTemplate("일반적인 수준의 청결도입니다. (평점: %d점)", Tag.TOILET_CLEAN,3,3),
            new ReviewTemplate("조명이 조금 어둡습니다. (평점: %d점)", Tag.BRIGHT_LIGHTING,3,3),
            new ReviewTemplate("손 세정제가 조금 부족했어요. (평점: %d점)", Tag.ENOUGH_HANDSOAP,3,3),
            new ReviewTemplate("변기에 물기가 조금 있었어요. (평점: %d점)", Tag.TOILET_CLEAN,3,3),
            new ReviewTemplate("세면대는 깨끗했지만 바닥에 물이 있었어요. (평점: %d점)", Tag.SINK_CLEAN,3,3),
            new ReviewTemplate("환기는 보통입니다. (평점: %d점)", Tag.GOOD_VENTILATION,3,3),
            new ReviewTemplate("조명이 노란빛입니다. (평점: %d점)", Tag.BRIGHT_LIGHTING,3,3),
            new ReviewTemplate("비누는 있었지만 부족해 보였습니다. (평점: %d점)", Tag.ENOUGH_HANDSOAP,3,3),
            new ReviewTemplate("깔끔한 편입니다. (평점: %d점)", Tag.TOILET_CLEAN,3,3),
            new ReviewTemplate("거울은 깨끗하지만 머리카락이 있었어요. (평점: %d점)", Tag.SINK_CLEAN,3,3),
            new ReviewTemplate("환기가 약간 부족합니다. (평점: %d점)", Tag.GOOD_VENTILATION,3,3),
            new ReviewTemplate("조명이 너무 밝아요. (평점: %d점)", Tag.BRIGHT_LIGHTING,3,3),
            new ReviewTemplate("휴지는 충분했어요. (평점: %d점)", Tag.ENOUGH_HANDSOAP,3,3),

            // 부정 리뷰 15개
            new ReviewTemplate("쓰레기통이 넘쳐흘렀습니다. (평점: %d점)", Tag.TRASH_OVERFLOW,1,2),
            new ReviewTemplate("바닥이 너무 더러웠어요. (평점: %d점)", Tag.DIRTY_FLOOR,1,2),
            new ReviewTemplate("거울에 얼룩이 많아요. (평점: %d점)", Tag.DIRTY_MIRROR,1,3),
            new ReviewTemplate("휴지가 없었습니다. (평점: %d점)", Tag.NO_TOILET_PAPER,1,2),
            new ReviewTemplate("악취가 심했습니다. (평점: %d점)", Tag.BAD_ODOR,1,2),
            new ReviewTemplate("칸막이 잠금장치가 고장났어요. (평점: %d점)", Tag.TOILET_CLEAN,1,1),
            new ReviewTemplate("세면대에 머리카락이 많았어요. (평점: %d점)", Tag.SINK_CLEAN,1,2),
            new ReviewTemplate("핸드 드라이어가 작동하지 않았습니다. (평점: %d점)", Tag.ENOUGH_HANDSOAP,1,2),
            new ReviewTemplate("입구에서부터 지린내가 났습니다. (평점: %d점)", Tag.BAD_ODOR,1,1),
            new ReviewTemplate("조명이 깜빡거렸습니다. (평점: %d점)", Tag.BRIGHT_LIGHTING,1,2),
            new ReviewTemplate("바닥에 소변 자국이 있었습니다. (평점: %d점)", Tag.DIRTY_FLOOR,1,1),
            new ReviewTemplate("전반적으로 지저분했습니다. (평점: %d점)", Tag.BAD_ODOR,1,1),
            new ReviewTemplate("휴지가 없었고 안내문구도 없었습니다. (평점: %d점)", Tag.NO_TOILET_PAPER,1,2),
            new ReviewTemplate("쓰레기가 치워지지 않았어요. (평점: %d점)", Tag.TRASH_OVERFLOW,1,2),
            new ReviewTemplate("거울이 너무 더러웠습니다. (평점: %d점)", Tag.DIRTY_MIRROR,1,2)
    );

    @PostConstruct
    @Transactional
    public void init() {

        if (reviewRepository.count() > 0) {
            log.info("리뷰 데이터 이미 존재. 초기화 생략.");
            return;
        }

        List<User> users = userRepository.findAllById(USER_IDS);
        if (users.size() < USER_IDS.size()) {
            log.warn("필요한 유저 1~7 중 누락된 유저 있음. 초기화 중단.");
            return;
        }

        List<Toilet> toilets = toiletRepository.findAll();
        if (toilets.isEmpty()) {
            log.warn("화장실 데이터 없음. 초기화 중단.");
            return;
        }

        List<User> maleUsers = users.stream().filter(u -> u.getGender() == Gender.M).collect(Collectors.toList());
        List<User> femaleUsers = users.stream().filter(u -> u.getGender() == Gender.F).collect(Collectors.toList());

        final int MALE_REVIEW_TARGET = maleUsers.size();   // 3명
        final int FEMALE_REVIEW_TARGET = femaleUsers.size(); // 4명

        int reviewCount = 0;

        for (Toilet toilet : toilets) {

            List<User> targetUsers;
            int targetReviewCount;

            if (toilet.getGender() == Gender.M) {
                targetUsers = maleUsers;
                targetReviewCount = MALE_REVIEW_TARGET;
            } else {
                targetUsers = femaleUsers;
                targetReviewCount = FEMALE_REVIEW_TARGET;
            }

            if (targetUsers.isEmpty()) continue;

            int localUserIndex = 0;
            int createdForThisToilet = 0;

            while (createdForThisToilet < targetReviewCount) {

                User currentUser = targetUsers.get(localUserIndex % targetUsers.size());
                ReviewTemplate template = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
                int starValue = random.nextInt(template.maxStar() - template.minStar() + 1) + template.minStar();

                String content = String.format(template.description(), starValue)
                        .replace("[STATION]", toilet.getName());

                // ──────────────────────────────────────────
                // ⭐ 태그: 반드시 1~2개
                // ──────────────────────────────────────────
                List<Tag> tags = new ArrayList<>();
                tags.add(template.mainTag());

                List<Tag> candidates = POSITIVE_TAGS.contains(template.mainTag())
                        ? new ArrayList<>(POSITIVE_TAGS)
                        : new ArrayList<>(NEGATIVE_TAGS);

                candidates.remove(template.mainTag());
                Collections.shuffle(candidates);

                // 50% 확률로 추가 태그 1개
                if (random.nextDouble() < 0.5 && !candidates.isEmpty()) {
                    tags.add(candidates.get(0));
                }

                // ──────────────────────────────────────────
                // ⭐ 장애인 화장실: 5% 확률
                // ──────────────────────────────────────────
                boolean isDis = random.nextDouble() < 0.05;

                Review review = Review.builder()
                        .star((double) starValue)
                        .description(content)
                        .toilet(toilet)
                        .user(currentUser)
                        .tag(tags)
                        .isDis(isDis)
                        .build();

                reviewRepository.save(review);
                reviewCount++;
                createdForThisToilet++;

                currentUser.addReview();
                userRepository.save(currentUser);

                toilet.updateReviewStats(starValue);
                toiletRepository.save(toilet);

                userRankService.evictUserRate(currentUser.getId());

                localUserIndex++;
            }
        }

        log.info("리뷰 생성 완료: 총 {}개 생성됨.", reviewCount);
    }
}
