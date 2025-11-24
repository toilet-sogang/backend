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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("ToiletDataLoader")
public class ReviewDataLoader {

    private final ReviewRepository reviewRepository;
    private final ToiletRepository toiletRepository;     // 화장실 통계 저장을 위해 필요
    private final UserRepository userRepository;         // 사용자 리뷰 개수 저장을 위해 필요
    private final UserRankService userRankService;

    // 가정된 사용자 ID 목록 (ID 1부터 7)
    private static final List<Long> USER_IDS = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L);
    private final Random random = new Random();

    // ------------------- 태그 그룹 정의 -------------------
    private static final List<Tag> POSITIVE_TAGS = Arrays.asList(
            Tag.TOILET_CLEAN, Tag.SINK_CLEAN, Tag.GOOD_VENTILATION, Tag.ENOUGH_HANDSOAP, Tag.BRIGHT_LIGHTING
    );
    private static final List<Tag> NEGATIVE_TAGS = Arrays.asList(
            Tag.TRASH_OVERFLOW, Tag.DIRTY_FLOOR, Tag.DIRTY_MIRROR, Tag.NO_TOILET_PAPER, Tag.BAD_ODOR
    );

    // ------------------- 리뷰 문구 및 태그 데이터 구조 -------------------
    private record ReviewTemplate(String description, Tag mainTag, int minStar, int maxStar) {}
    private static final List<ReviewTemplate> TEMPLATES = Arrays.asList(
            // --- 긍정적 리뷰 (4~5점) - 총 15개 ---
            new ReviewTemplate("변기 위생 상태가 매우 훌륭합니다. [STATION]역 최고! (청결) (평점: %d점)", Tag.TOILET_CLEAN, 4, 5),
            new ReviewTemplate("세면대가 물기 없이 깨끗해서 좋았어요. 거울도 맑아요. (평점: %d점)", Tag.SINK_CLEAN, 4, 5),
            new ReviewTemplate("냄새 없이 쾌적했습니다. 환기가 잘 되어 만족스러워요. (평점: %d점)", Tag.GOOD_VENTILATION, 4, 5),
            new ReviewTemplate("조명이 밝아서 이용하기 편했습니다. 전체적으로 청결했어요. (평점: %d점)", Tag.BRIGHT_LIGHTING, 4, 5),
            new ReviewTemplate("손 세정제가 넉넉했고, 휴지도 충분했습니다. 기본 관리가 잘 돼요. (평점: %d점)", Tag.ENOUGH_HANDSOAP, 4, 5),
            new ReviewTemplate("깨끗하고 넓어서 좋았습니다. 다음에도 이용할 의향 있습니다. (평점: %d점)", Tag.TOILET_CLEAN, 4, 5),
            new ReviewTemplate("화장실이 쾌적하고 밝아서 안심하고 이용했어요. 특히 냄새가 없어서 좋았습니다. (평점: %d점)", Tag.GOOD_VENTILATION, 5, 5),
            new ReviewTemplate("세면대 관리가 잘 되어있네요. 물도 잘 나오고 거품 비누도 충분합니다. (평점: %d점)", Tag.SINK_CLEAN, 4, 5),
            new ReviewTemplate("아주 기본적인 것들이 잘 갖춰져 있어 만족스럽습니다. 밝고 깔끔해요. (평점: %d점)", Tag.BRIGHT_LIGHTING, 4, 5),
            new ReviewTemplate("이용객이 많은 역인데도 청결 상태를 잘 유지하고 있어요. 칭찬합니다! (평점: %d점)", Tag.TOILET_CLEAN, 5, 5),
            new ReviewTemplate("칸막이 공간이 넓어서 편안하게 이용했습니다. 짐 놓을 공간도 충분해요. (평점: %d점)", Tag.TOILET_CLEAN, 4, 5),
            new ReviewTemplate("휴지통이 비워져 있고 관리가 철저했습니다. 위생 상태 최고! (평점: %d점)", Tag.TOILET_CLEAN, 5, 5),
            new ReviewTemplate("따뜻한 물이 잘 나와서 손 씻기 좋았습니다. 세면대 시설 훌륭해요. (평점: %d점)", Tag.SINK_CLEAN, 4, 5),
            new ReviewTemplate("다른 역과 비교했을 때 확실히 악취가 덜 납니다. 환기 시설이 좋은 듯! (평점: %d점)", Tag.GOOD_VENTILATION, 4, 5),
            new ReviewTemplate("센서 등 작동이 빨라서 편리했습니다. 밝고 안전한 느낌이에요. (평점: %d점)", Tag.BRIGHT_LIGHTING, 5, 5),

            // --- 보통 리뷰 (3점) - 총 15개 ---
            new ReviewTemplate("평범한 지하철 화장실입니다. 눈에 띄게 더럽지도, 깨끗하지도 않아요. (평점: %d점)", Tag.ENOUGH_HANDSOAP, 3, 3),
            new ReviewTemplate("큰 문제는 없으나, 약간 꿉꿉한 냄새가 납니다. 환기를 조금 더 신경 써야 할 것 같아요. (평점: %d점)", Tag.GOOD_VENTILATION, 3, 3),
            new ReviewTemplate("일반적인 수준의 청결도입니다. 사용하기에 불편함은 없었어요. (평점: %d점)", Tag.TOILET_CLEAN, 3, 3),
            new ReviewTemplate("조명이 조금 어두운 편이지만, 사용에 지장은 없습니다. (평점: %d점)", Tag.BRIGHT_LIGHTING, 3, 3),
            new ReviewTemplate("손 세정제가 조금 부족한 느낌이었지만, 일단 있었습니다. (평점: %d점)", Tag.ENOUGH_HANDSOAP, 3, 3),
            new ReviewTemplate("사용에 문제는 없었지만, 청소 시간이 임박했는지 변기에 물기가 좀 남아있었어요. (평점: %d점)", Tag.TOILET_CLEAN, 3, 3),
            new ReviewTemplate("세면대 자체는 깨끗했지만, 바닥에 물이 흥건해서 불편했어요. (평점: %d점)", Tag.SINK_CLEAN, 3, 3),
            new ReviewTemplate("환기는 보통입니다. 나쁘지 않은데 아주 상쾌하지도 않아요. (평점: %d점)", Tag.GOOD_VENTILATION, 3, 3),
            new ReviewTemplate("조명이 약간 노란빛이라 전체적으로 어두워 보입니다. 기능상 문제 없음. (평점: %d점)", Tag.BRIGHT_LIGHTING, 3, 3),
            new ReviewTemplate("비누는 있었지만 거의 다 떨어져 가고 있었습니다. 곧 리필이 필요해 보여요. (평점: %d점)", Tag.ENOUGH_HANDSOAP, 3, 3),
            new ReviewTemplate("냄새도 없고 깔끔한 편입니다. 특별히 인상적인 부분은 없네요. (평점: %d점)", Tag.TOILET_CLEAN, 3, 3),
            new ReviewTemplate("거울은 깨끗했지만 세면대 주변에 머리카락이 몇 가닥 보였어요. (평점: %d점)", Tag.SINK_CLEAN, 3, 3),
            new ReviewTemplate("문을 열었을 때 냄새는 없었지만, 문을 닫으니 환기가 잘 안 되는 느낌입니다. (평점: %d점)", Tag.GOOD_VENTILATION, 3, 3),
            new ReviewTemplate("화장실 자체는 새것 같은데 조명이 너무 밝아 눈이 부십니다. (평점: %d점)", Tag.BRIGHT_LIGHTING, 3, 3),
            new ReviewTemplate("휴지는 충분했지만, 핸드타월은 없었습니다. 세정제는 있었어요. (평점: %d점)", Tag.ENOUGH_HANDSOAP, 3, 3),


            // --- 부정적 리뷰 (1~2점) - 총 15개 ---
            new ReviewTemplate("쓰레기통이 가득 차서 쓰레기가 넘쳐흐르고 있었습니다. 관리 부탁드려요. (평점: %d점)", Tag.TRASH_OVERFLOW, 1, 2),
            new ReviewTemplate("바닥에 물기가 많고 휴지가 널려있어 불쾌했어요. 청소가 시급합니다. (평점: %d점)", Tag.DIRTY_FLOOR, 1, 2),
            new ReviewTemplate("거울에 얼룩이 너무 많고 조명이 어두워서 지저분해 보입니다. (평점: %d점)", Tag.DIRTY_MIRROR, 1, 3),
            new ReviewTemplate("가장 중요한 휴지가 없었습니다. 매우 실망입니다. (평점: %d점)", Tag.NO_TOILET_PAPER, 1, 2),
            new ReviewTemplate("역 전체에 심한 악취가 나서 급히 이용하고 나왔습니다. 코를 막고 사용했어요. (평점: %d점)", Tag.BAD_ODOR, 1, 2),
            new ReviewTemplate("칸막이 문 잠금 장치가 고장나서 이용하지 못했습니다. 당장 수리해주세요. (평점: %d점)", Tag.TOILET_CLEAN, 1, 1),
            new ReviewTemplate("세면대에 머리카락과 물때가 끼어있었습니다. 청결도가 매우 낮아요. (평점: %d점)", Tag.SINK_CLEAN, 1, 2),
            new ReviewTemplate("핸드 드라이어가 작동하지 않아 손을 털고 나왔습니다. 무용지물이에요. (평점: %d점)", Tag.ENOUGH_HANDSOAP, 1, 2),
            new ReviewTemplate("화장실 입구부터 지린내가 너무 심해서 구토가 나올 것 같았습니다. 최악의 악취. (평점: %d점)", Tag.BAD_ODOR, 1, 1),
            new ReviewTemplate("조명이 깜빡거리고 너무 어두워서 불안했습니다. 안전 문제도 있어 보여요. (평점: %d점)", Tag.BRIGHT_LIGHTING, 1, 2),
            new ReviewTemplate("바닥에 소변 자국이 그대로 남아있었습니다. 발 디딜 틈이 없네요. (평점: %d점)", Tag.DIRTY_FLOOR, 1, 1),
            new ReviewTemplate("역 화장실 관리가 이 정도일 줄은 몰랐습니다. 역한 냄새 때문에 이용을 포기했어요. (평점: %d점)", Tag.BAD_ODOR, 1, 1),
            new ReviewTemplate("휴지가 없는 것도 문제지만, 안내 문구조차 없어 불편했습니다. (평점: %d점)", Tag.NO_TOILET_PAPER, 1, 2),
            new ReviewTemplate("휴지통이 아닌 곳에 쓰레기가 버려져 있었어요. 관리가 안 되는 것 같습니다. (평점: %d점)", Tag.TRASH_OVERFLOW, 1, 2),
            new ReviewTemplate("거울이 너무 더러워서 닦을 수조차 없었습니다. 먼지가 수북해요. (평점: %d점)", Tag.DIRTY_MIRROR, 1, 2)
    );

    // ------------------- 초기화 로직 -------------------

    @PostConstruct
    @Transactional
    public void init() {
        if (reviewRepository.count() > 0) {
            log.info("✅ 리뷰 테이블이 이미 초기화되었습니다. 데이터 삽입을 건너킵니다.");
            return;
        }

        List<User> users = userRepository.findAllById(USER_IDS);
        if (users.size() < USER_IDS.size()) {
            log.warn("⚠️ 필요한 사용자 (ID 1-7)가 모두 발견되지 않았습니다. 리뷰 초기화를 건너킵니다.");
            return;
        }

        List<Toilet> toilets = toiletRepository.findAll();
        if (toilets.isEmpty()) {
            log.warn("⚠️ 데이터베이스에 화장실 데이터가 없습니다. 리뷰 초기화를 건너킵니다.");
            return;
        }

        // 1. 사용자 리스트를 성별로 분리
        List<User> maleUsers = users.stream()
                .filter(u -> u.getGender() == Gender.M)
                .collect(Collectors.toList());
        List<User> femaleUsers = users.stream()
                .filter(u -> u.getGender() == Gender.F)
                .collect(Collectors.toList());

        // 화장실당 리뷰 목표 개수를 유저 수에 따라 동적으로 설정
        final int MALE_REVIEW_TARGET = maleUsers.size();   // 3개
        final int FEMALE_REVIEW_TARGET = femaleUsers.size(); // 4개

        int reviewCount = 0;

        log.info("⭐ 리뷰 초기화 시작: 총 {}개의 화장실, 남성 유저 {}명, 여성 유저 {}명.",
                toilets.size(), maleUsers.size(), femaleUsers.size());
        log.info("🎯 목표 리뷰: 남성 화장실당 {}개, 여성 화장실당 {}개. 총 목표 개수: {}개",
                MALE_REVIEW_TARGET, FEMALE_REVIEW_TARGET, MALE_REVIEW_TARGET * 301 + FEMALE_REVIEW_TARGET * 301);

        List<User> targetUsers;
        int targetReviewCount;
        int localUserIndex;

        for (Toilet toilet : toilets) {

            Gender toiletGender = toilet.getGender();

            // 2. Gender.M과 Gender.F를 명확히 분리하여 리뷰 목표 개수 할당
            if (toiletGender == Gender.M) {
                targetUsers = maleUsers;
                targetReviewCount = MALE_REVIEW_TARGET;
            } else if (toiletGender == Gender.F) {
                targetUsers = femaleUsers;
                targetReviewCount = FEMALE_REVIEW_TARGET;
            } else {
                log.warn("⚠️ 화장실 {}는 예상치 못한 성별 값({})입니다. 스킵합니다.", toilet.getId(), toiletGender);
                continue;
            }

            if (targetUsers.isEmpty()) {
                log.warn("⚠️ 화장실 {} (gender={}) 에 대해 일치하는 성별 유저 풀이 비어있습니다. 스킵합니다.", toilet.getId(), toiletGender);
                continue;
            }

            int createdForThisToilet = 0;
            localUserIndex = 0; // 로컬 인덱스 초기화

            // 화장실당 목표 개수(3개 또는 4개)까지 리뷰 생성
            while (createdForThisToilet < targetReviewCount) {

                // 유저 순환 및 리뷰 정보 생성
                User currentUser = targetUsers.get(localUserIndex % targetUsers.size());
                ReviewTemplate template = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
                int starValue = random.nextInt(template.maxStar() - template.minStar() + 1) + template.minStar();

                String content = String.format(template.description(), starValue)
                        .replace("[STATION]", toilet.getName());

                List<Tag> tags = new ArrayList<>();
                // 메인 태그 추가
                tags.add(template.mainTag());

                // 3. 50% 확률로 0~1개의 추가 태그 선택 (생략하지 않음)
                if (random.nextDouble() < 0.5) {
                    List<Tag> potentialTags;
                    if (POSITIVE_TAGS.contains(template.mainTag())) {
                        potentialTags = new ArrayList<>(POSITIVE_TAGS);
                    } else {
                        potentialTags = new ArrayList<>(NEGATIVE_TAGS);
                    }
                    potentialTags.remove(template.mainTag());
                    Collections.shuffle(potentialTags);
                    if (!potentialTags.isEmpty()) {
                        tags.add(potentialTags.get(0));
                    }
                }

                Review review = Review.builder()
                        .star((double) starValue)
                        .description(content)
                        .toilet(toilet)
                        .user(currentUser)
                        .tag(tags)
                        .isDis(random.nextDouble() < 0.05)
                        .build();

                reviewRepository.save(review);
                reviewCount++;
                createdForThisToilet++;

                // ----------------------------------------------------
                // ✨ 사용자 및 화장실 통계 업데이트 및 DB 반영
                // ----------------------------------------------------

                // 1. 사용자 리뷰 개수 업데이트 및 DB 반영
                currentUser.addReview();
                userRepository.save(currentUser);

                // 2. 화장실 통계 (총 리뷰 개수, 평균 별점) 업데이트 및 DB 반영
                toilet.updateReviewStats((double) starValue);
                toiletRepository.save(toilet); // 👈 DB 반영

                // 3. 랭킹 캐시 무효화 (상위 순위 재계산을 위해)
                userRankService.evictUserRate(currentUser.getId());

                localUserIndex++;
            }
        }

        log.info("✅ 리뷰 초기화 완료. 총 {}개의 리뷰가 성공적으로 생성되었습니다.", reviewCount);
        log.info("➡️ 생성된 리뷰 개수는 {}개입니다. (목표 리뷰 개수 2107개)", reviewCount);
    }
}