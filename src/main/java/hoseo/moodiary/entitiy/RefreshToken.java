package hoseo.moodiary.entitiy;

import hoseo.moodiary.entitiy.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh Token 엔티티 — access token 보다 긴 수명 (2주) 으로 발급되어 access 갱신에 사용.
 *
 * <p><b>저장 정책</b> — 클라이언트가 들고 있는 raw token 은 DB 에 저장하지 않고 SHA-256 hex 만 저장.
 * DB 가 노출되어도 raw token 은 복원 불가. 검증 시 들어온 raw token 을 같은 방식으로 hash 후 비교.
 *
 * <p><b>Rotation</b> — refresh 호출마다 기존 토큰을 {@code revokedAt} 으로 무효화하고 새 토큰 발급.
 * 탈취된 refresh 가 한 번만 유효하도록 보장.
 *
 * <p><b>UNIQUE 인덱스 (token_hash)</b> — {@code @UniqueConstraint} 명시 + DB 정책상 {@code ddl-auto:update}
 * 가 보장 X 라 운영 머지 후 수동 확인 권장. ([CLAUDE.md DB 정책])
 *
 * <p><b>일반 인덱스 (user_id)</b> — 전체 device 로그아웃 (deleteByUserId) 미래 확장 + 사용자별 활성 토큰
 * 조회 시 활용.
 */
@Entity
@Table(
        name = "refresh_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_refresh_token_hash",
                columnNames = "refresh_token_hash"
        ),
        indexes = @Index(
                name = "idx_refresh_token_user_id",
                columnList = "user_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @Column(name = "refresh_token_id")
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** SHA-256 hex = 64 chars. raw token 은 저장하지 않는다. */
    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** null 이면 유효. revoke / rotate 시 시각 기록. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Builder
    public RefreshToken(UUID userId, String tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
