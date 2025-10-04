package hwalibo.toilet.init;

import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.InOut;
import hwalibo.toilet.respository.toilet.ToiletRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToiletDataLoader {

    private final ToiletRepository toiletRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (toiletRepository.count() > 0) {
            log.info("✅ Toilet table already initialized. Skipping CSV import.");
            return;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/data/toilet_preprocessed.csv"),
                        StandardCharsets.UTF_8))) {

            String line;
            br.readLine(); // header skip
            int count = 0;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");

                // 방어적 파싱 (빈 값이면 0 or 0.0)
                String name = tokens[0].trim();
                Integer lineNum = safeParseInt(tokens[1]);
                Gender gender = safeParseEnum(tokens[2], Gender.class);
                InOut inOut = safeParseEnum(tokens[3], InOut.class);
                Double latitude = safeParseDouble(tokens[4]);
                Double longitude = safeParseDouble(tokens[5]);
                Integer numGate = safeParseInt(tokens[6]);
                Integer numBigToilet = safeParseInt(tokens[7]);
                Integer numSmallToilet = safeParseInt(tokens[8]);
                Integer numReview = safeParseInt(tokens[9]);
                Double star = safeParseDouble(tokens[10]);

                Toilet toilet = Toilet.builder()
                        .name(name)
                        .line(lineNum)
                        .gender(gender)
                        .inOut(inOut)
                        .latitude(latitude)
                        .longitude(longitude)
                        .numGate(numGate)
                        .numBigToilet(numBigToilet)
                        .numSmallToilet(numSmallToilet)
                        .numReview(numReview)
                        .star(star)
                        .build();

                toiletRepository.save(toilet);
                count++;
            }

            log.info("✅ {} toilets successfully loaded into database.", count);

        } catch (Exception e) {
            log.error("❌ Error loading toilet data", e);
        }
    }

    // ------------------- Helper methods -------------------

    private Integer safeParseInt(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Double safeParseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0.0;
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private <E extends Enum<E>> E safeParseEnum(String value, Class<E> enumClass) {
        try {
            if (value == null || value.trim().isEmpty()) return null;
            return Enum.valueOf(enumClass, value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

