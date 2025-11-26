package hwalibo.toilet.dto.review.response.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.Tag;
import hwalibo.toilet.dto.review.photo.response.ImageDto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MyReviewResponse {

    private final Long id;
    private final String name;
    private final String gender;
    private final Integer line;
    private final String description;
    private final Integer star;
    private final List<ImageDto> photo;
    private final List<String> tag;
    @JsonProperty("isDis")
    private final boolean isDis;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime updatedAt;

    public MyReviewResponse(Long id, String name, Gender gender, Integer line,
                            String description, Integer star, List<ImageDto> photo, List<Tag> tag, boolean isDis,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.gender = gender.name();
        this.line = line;
        this.description = description;
        this.star = star;
        this.photo = photo;
        this.tag = tag.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        this.isDis = isDis;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
