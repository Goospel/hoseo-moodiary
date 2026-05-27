package hoseo.moodiary.entitiy;

import hoseo.moodiary.entitiy.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * 일기 한 건에 대한 비동기 AI 추론 결과 (1:1).
 *
 * <p><b>흐름</b>:
 * <ol>
 *   <li>{@code POST /post} 가 일기 저장과 같은 트랜잭션에 {@code AiResponse(PENDING)} row 를 만든다.</li>
 *   <li>컨트롤러가 commit 후 {@code @Async} 메서드로 추론을 트리거.</li>
 *   <li>성공: {@link #markDone(String, String)}. 실패: {@link #markFailed(String)}.</li>
 * </ol>
 *
 * <p><b>관계</b>: {@link Post} 와 단방향 1:1. {@code post_id} 에 unique 제약을 걸어 한 글당 한 응답만.
 * cascade 는 엔티티에 매핑하지 않고 {@code AiResponseRepository.deleteByPost_Id()} 를 서비스 레이어에서 호출 —
 * Post 도메인이 AI 모듈을 모르게 한다.
 *
 * <p><b>스키마</b>: {@code ddl-auto: update} 는 unique 제약 자동 생성을 보장하지 않으므로
 * {@code @Table(uniqueConstraints=...)} 에 명시. 운영 머지 후 {@code SHOW INDEX FROM ai_response;} 로 확인.
 *
 * <p><b>인코딩</b>: {@code emoji} 컬럼은 utf8mb4 가 필수 (RDS {@code character_set_database} 확인 — 운영 머지 전 필수).
 */
@Entity
@Table(
        name = "ai_response",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_response_post_id", columnNames = "post_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiResponse extends BaseEntity {

    @Id
    @Column(name = "ai_response_id")
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_response_status", nullable = false, length = 16)
    private AiResponseStatus status;

    @Column(name = "ai_response_content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_response_emoji")
    private String emoji;

    @Column(name = "ai_response_error_message")
    private String errorMessage;

    @Builder
    public AiResponse(Post post) {
        this.post = post;
        this.status = AiResponseStatus.PENDING;
    }

    /** PENDING → DONE 전이. */
    public void markDone(String content, String emoji) {
        this.status = AiResponseStatus.DONE;
        this.content = content;
        this.emoji = emoji;
    }

    /** PENDING → FAILED 전이. */
    public void markFailed(String errorMessage) {
        this.status = AiResponseStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
