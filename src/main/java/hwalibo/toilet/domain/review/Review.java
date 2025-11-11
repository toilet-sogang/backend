package hwalibo.toilet.domain.review;

import hwalibo.toilet.domain.toilet.Toilet;
import hwalibo.toilet.domain.type.Tag;
import hwalibo.toilet.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // 생성/수정 시간 자동 감지를 위한 리스너
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✨ 다대일(N:1) 관계 설정: Review(N) : Toilet(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toilet_id", nullable = false)
    private Toilet toilet;

    // ✨ 다대일(N:1) 관계 설정: Review(N) : User(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT") // 더 긴 텍스트를 저장할 수 있도록 TEXT 타입으로 설정
    private String description;

    @Column(nullable = false)
    private Double star;

    // S3 이미지 URL 리스트 (0~2개 저장)
    @Builder.Default
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC") // 이미지 순서 정렬
    private List<ReviewImage> reviewImages = new ArrayList<>();

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer good; // '좋아요' 수

    // Tag Enum 리스트
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag")
    private List<Tag> tag;

    private boolean isDis; // 장애인 화장실 여부

    @CreatedDate // 엔티티 생성 시 시간 자동 저장
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 엔티티 수정 시 시간 자동 저장
    private LocalDateTime updatedAt;

    public void updateIsDis(boolean isDis) {
        this.isDis = isDis;
    }
}