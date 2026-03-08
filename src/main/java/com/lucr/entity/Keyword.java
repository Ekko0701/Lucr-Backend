package com.lucr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Keyword Entity — 뉴스에서 추출된 키워드 정보
 *
 * <h3>테이블 설계</h3>
 * <pre>
 * keywords
 * ├── id         (PK, UUID)
 * ├── word       (UNIQUE, VARCHAR 100)
 * ├── frequency  (NOT NULL, DEFAULT 1)
 * ├── created_at
 * └── updated_at
 * </pre>
 *
 * <p>
 * 같은 단어("삼성전자")가 여러 뉴스에서 추출되더라도
 * 하나의 레코드를 공유하고 frequency를 증가시키는 구조를 사용합니다.
 * </p>
 *
 * <p>
 * 참고: NewsKeyword 연관관계는 STEP6(NewsKeyword 엔티티 추가)에서 연결합니다.
 * </p>
 *
 * @author Ekko0701
 * @since 2026-03-08
 */
@Entity
@Table(
        name = "keywords",
        indexes = {
                @Index(name = "idx_keyword_word", columnList = "word"),
                @Index(name = "idx_keyword_frequency", columnList = "frequency DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 키워드 문자열 (중복 방지를 위해 UNIQUE)
     */
    @Column(name = "word", nullable = false, unique = true, length = 100)
    private String word;

    /**
     * 전체 뉴스에서 이 키워드가 등장한 누적 횟수
     */
    @Column(name = "frequency", nullable = false)
    @Builder.Default
    private Integer frequency = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 키워드 등장 횟수 증가
     */
    public void incrementFrequency() {
        this.frequency++;
    }
}
