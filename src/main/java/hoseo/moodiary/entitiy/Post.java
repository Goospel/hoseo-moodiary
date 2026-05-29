package hoseo.moodiary.entitiy;

import hoseo.moodiary.entitiy.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @Column(name = "post_id")
    @UuidGenerator
    private UUID id;

    @Column(name = "post_title")
    private String title;

    @Column(name = "post_content")
    private String content;

    /**
     * 일기 날짜 (entry date). 사용자가 "이 일기는 어느 날의 일인가" 명시하는 값 —
     * {@code createdAt} (작성 시점, BaseEntity 자동) 과 별개. 지나간 날짜에 대한 일기 작성 시나리오
     * ("어제 일을 오늘 적기" / "휴가 다녀와서 며칠치 한꺼번에 입력") 를 지원한다.
     *
     * <p>입력 누락 시 서비스 레이어에서 {@code LocalDate.now()} 로 폴백 — 기본 동작은 "오늘 일기".
     * 미래 날짜 server-side 검증은 박지 않는다 (의도적 — 필요해지면 {@code @PastOrPresent} 추가).
     */
    @Column(name = "post_date", nullable = false)
    private LocalDate postDate;

    /**
     * 작성자. 지연 로딩 — Post를 불러와도 User row를 즉시 조회하지 않는다.
     * 소유권 검사용 {@code post.getUser().getId()}는 FK 컬럼만 읽어 추가 쿼리 없이 동작.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Post(String title, String content, LocalDate postDate, User user) {
        this.title = title;
        this.content = content;
        this.postDate = postDate;
        this.user = user;
    }

    public void update(String title, String content, LocalDate postDate) {
        this.title = title;
        this.content = content;
        this.postDate = postDate;
    }

    /** 소유 확인 헬퍼 — 서비스 레이어에서 사용. */
    public boolean isOwnedBy(UUID userId) {
        return this.user != null && this.user.getId().equals(userId);
    }
}
