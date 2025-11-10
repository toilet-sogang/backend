package hwalibo.toilet.dto.review.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.Tag;
import hwalibo.toilet.dto.review.photo.response.ImageDto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MyReviewResponse {

    private final Long id;          // 리뷰 ID
    private final String name;        // 화장실 이름 (Toilet.name)
    private final String gender;      // 화장실 성별 ("MALE" / "FEMALE")
    private final Integer numGate;    // 화장실 출구 번호
    private final String description;        // 리뷰 상세 내용
    private final Integer star;       // 별점
    private final List<ImageDto> photo; // 사진 URL 리스트
    private final List<String> tag;   // 태그 코드값 리스트 ("TOILET_CLEAN" 등)
    private final boolean isDis;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime updatedAt;

    /**
     * JPA @Query에서 'SELECT new...' 구문을 위한 생성자입니다.
     * JPQL의 필드 순서와 타입이 이 생성자의 파라미터와 정확히 일치해야 합니다.
     */
    public MyReviewResponse(Long id, String name, Gender gender, Integer numGate,
                            String description, Integer star, List<ImageDto> photo, List<Tag> tag, boolean isDis,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.gender = gender.name(); // Enum -> String 변환
        this.numGate = numGate;
        this.description = description;
        this.star = star;
        this.photo = photo;
        this.tag = tag.stream()       // List<ReviewTag> -> List<String> 변환
                .map(Enum::name)
                .collect(Collectors.toList());
        this.isDis = isDis;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
