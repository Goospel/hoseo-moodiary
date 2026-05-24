package hoseo.moodiary.entitiy;

import hoseo.moodiary.entitiy.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * 회원 엔티티.
 *
 * <p>테이블명은 <b>users</b>. MySQL의 {@code USER} 예약어 충돌을 피하기 위해 단수형 {@code user}를 쓰지 않는다.
 * {@link #email}, {@link #nickname}은 DB 레벨에서 unique 제약을 건다 (서비스 레이어 사전 체크와 이중 안전망).
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "user_email"),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = "user_nickname")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @Column(name = "user_id")
    @UuidGenerator
    private UUID id;

    @Column(name = "user_email", nullable = false, length = 254)
    private String email;

    /**
     * BCrypt로 해시된 비밀번호. 평문은 절대 저장하지 않는다.
     * BCrypt 출력 길이는 60자(prefix 포함)지만 알고리즘 변경 여지를 두기 위해 100자로 잡는다.
     */
    @Column(name = "user_password", nullable = false, length = 100)
    private String password;

    @Column(name = "user_nickname", nullable = false, length = 20)
    private String nickname;

    @Builder
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
