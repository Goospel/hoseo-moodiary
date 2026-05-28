package hoseo.moodiary.entitiy;

import hoseo.moodiary.entitiy.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 *
 * <p><b>인증 경로</b>는 {@link #provider} 로 구분한다:
 * <ul>
 *   <li>{@code LOCAL} — 이메일 + BCrypt 비밀번호. {@code password} 필수, {@code providerId} null.</li>
 *   <li>{@code GOOGLE} — OAuth2. {@code password} null, {@code providerId} = Google 의 {@code sub} 클레임.</li>
 * </ul>
 *
 * <p><b>유니크 제약</b>:
 * <ul>
 *   <li>{@code uk_users_email} — 이메일은 시스템 전역에서 유일 (LOCAL ↔ OAuth2 양쪽 가입 차단 정책 — 같은 이메일 한 사용자만).</li>
 *   <li>{@code uk_users_nickname} — 닉네임도 전역 유일.</li>
 *   <li>{@code uk_users_provider_provider_id} — 같은 provider 안에서 providerId 유일 (LOCAL 끼리는 providerId=null 라 제약 의미 없음 — MySQL 에서 NULL 은 UNIQUE 충돌 안 함).</li>
 * </ul>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "user_email"),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = "user_nickname"),
                @UniqueConstraint(
                        name = "uk_users_provider_provider_id",
                        columnNames = {"user_provider", "user_provider_id"}
                )
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
     * BCrypt로 해시된 비밀번호.
     *
     * <p>{@link AuthProvider#LOCAL} 회원만 값을 가진다. OAuth2 회원 ({@code GOOGLE}) 은 {@code null}.
     * BCrypt 출력 길이는 60자(prefix 포함)지만 알고리즘 변경 여지를 두기 위해 100자로 잡는다.
     */
    @Column(name = "user_password", nullable = true, length = 100)
    private String password;

    @Column(name = "user_nickname", nullable = false, length = 20)
    private String nickname;

    /**
     * 인증 제공자 — LOCAL / GOOGLE. DB 에는 문자열 저장.
     *
     * <p>{@code nullable = false} 라도 기존 LOCAL 회원의 row 가 이미 운영에 있어서 ddl-auto: update 가 ALTER 시 default 가 필요.
     * Hibernate 가 default 를 박지 않으니 — 운영 머지 전 사후 DDL 로 NOT NULL DEFAULT 'LOCAL' 적용 필요 (PR body 의 운영 머지 전 필수 체크리스트).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_provider", nullable = false, length = 16)
    private AuthProvider provider;

    /**
     * Provider 측 user ID — LOCAL 이면 {@code null}, OAuth2 이면 Google {@code sub} 클레임.
     * 길이는 Google {@code sub} (~21자) 여유 두고 64자 (다른 provider 부활 대비).
     */
    @Column(name = "user_provider_id", nullable = true, length = 64)
    private String providerId;

    @Builder
    private User(String email, String password, String nickname, AuthProvider provider, String providerId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
    }

    /**
     * LOCAL 회원가입 — 이메일 + BCrypt 해시 + 닉네임.
     */
    public static User createLocal(String email, String encodedPassword, String nickname) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .build();
    }

    /**
     * OAuth2 회원가입 — provider 측 user ID + 이메일 + 닉네임. 비밀번호 없음.
     */
    public static User createOAuth2(AuthProvider provider, String providerId, String email, String nickname) {
        if (provider == AuthProvider.LOCAL) {
            throw new IllegalArgumentException("LOCAL 은 createLocal() 사용");
        }
        return User.builder()
                .email(email)
                .password(null)
                .nickname(nickname)
                .provider(provider)
                .providerId(providerId)
                .build();
    }
}
