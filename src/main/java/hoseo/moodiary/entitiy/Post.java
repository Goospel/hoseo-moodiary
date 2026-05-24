package hoseo.moodiary.entitiy;

import hoseo.moodiary.entitiy.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

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
     * 작성자. 지연 로딩 — Post를 불러와도 User row를 즉시 조회하지 않는다.
     * 소유권 검사용 {@code post.getUser().getId()}는 FK 컬럼만 읽어 추가 쿼리 없이 동작.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Post(String title, String content, User user) {
        this.title = title;
        this.content = content;
        this.user = user;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /** 소유 확인 헬퍼 — 서비스 레이어에서 사용. */
    public boolean isOwnedBy(UUID userId) {
        return this.user != null && this.user.getId().equals(userId);
    }
}
