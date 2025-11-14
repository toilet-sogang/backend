package hwalibo.toilet.domain.review;

import hwalibo.toilet.domain.type.ValidationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class ReviewImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus status = ValidationStatus.PENDING;

    @Column(nullable = false)
    private String url; // S3에 업로드된 이미지 URL

    @Column(nullable = false)
    private Integer sortOrder; // 이미지 순서 (대표 이미지 = 0)

    // URL 수정
    public void updateUrl(String newUrl) {
        this.url = newUrl;
    }

    public void approve() {
        this.status = ValidationStatus.APPROVED;
    }
}

