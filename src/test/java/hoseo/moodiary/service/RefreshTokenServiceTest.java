package hoseo.moodiary.service;

import hoseo.moodiary.config.JwtProperties;
import hoseo.moodiary.entitiy.RefreshToken;
import hoseo.moodiary.exception.InvalidRefreshTokenException;
import hoseo.moodiary.repository.RefreshTokenJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * RefreshTokenService 단위 테스트 — issue / rotate / revoke 의 핵심 분기.
 *
 * <p>raw token 은 클라이언트에만 전달되고 DB 에는 SHA-256 hex 만 저장된다.
 * 그래서 mock 으로 검증할 때 "saved.tokenHash == sha256(rawToken)" 임을 확인.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenJpaRepository refreshTokenRepository;

    private static final long REFRESH_EXPIRATION_MS = 1_209_600_000L; // 14d

    private final JwtProperties jwtProperties =
            new JwtProperties("test-secret-key-test-secret-key!", 3_600_000L, REFRESH_EXPIRATION_MS);

    private RefreshTokenService refreshTokenService() {
        return new RefreshTokenService(refreshTokenRepository, jwtProperties);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RefreshToken validToken(UUID userId, String rawToken) {
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();
        return token;
    }

    private static RefreshToken expiredToken(UUID userId, String rawToken) {
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        return token;
    }

    private static RefreshToken revokedToken(UUID userId, String rawToken) {
        RefreshToken token = validToken(userId, rawToken);
        token.revoke();
        return token;
    }

    @Nested
    @DisplayName("issue — 발급")
    class Issue {

        @Test
        @DisplayName("새 토큰 발급 + DB 에 SHA-256 hash 로 저장 + raw 반환")
        void success() {
            UUID userId = UUID.randomUUID();

            String rawToken = refreshTokenService().issue(userId);

            assertThat(rawToken).isNotBlank();
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getTokenHash()).isEqualTo(sha256(rawToken));
            // raw token 자체는 절대 DB 에 안 들어감
            assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
            // expires_at 이 미래 (2주 ± 짧은 시간)
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(13));
            assertThat(saved.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(15));
            assertThat(saved.isValid()).isTrue();
        }

        @Test
        @DisplayName("같은 사용자가 두 번 발급해도 서로 다른 토큰 — 매번 secure random")
        void uniquePerInvocation() {
            UUID userId = UUID.randomUUID();
            RefreshTokenService service = refreshTokenService();

            String first = service.issue(userId);
            String second = service.issue(userId);

            assertThat(first).isNotEqualTo(second);
        }
    }

    @Nested
    @DisplayName("rotate — 회전")
    class Rotate {

        @Test
        @DisplayName("유효한 토큰이면 기존 revoke + 새 발급 + RotationResult 반환")
        void success() {
            UUID userId = UUID.randomUUID();
            String rawToken = "old-raw-token";
            RefreshToken existing = validToken(userId, rawToken);
            given(refreshTokenRepository.findByTokenHash(sha256(rawToken)))
                    .willReturn(Optional.of(existing));

            RefreshTokenService.RotationResult result = refreshTokenService().rotate(rawToken);

            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.newRefreshToken()).isNotBlank();
            assertThat(result.newRefreshToken()).isNotEqualTo(rawToken);
            // 기존 토큰은 revoke 됨 — 동일 raw 로 두 번째 호출은 InvalidRefreshTokenException 보장
            assertThat(existing.isRevoked()).isTrue();
            // 새 토큰이 DB 에 저장됨
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("토큰이 존재하지 않으면 InvalidRefreshTokenException + 새 토큰 발급 안 함")
        void notFound() {
            given(refreshTokenRepository.findByTokenHash(sha256("ghost")))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService().rotate("ghost"))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("만료된 토큰이면 InvalidRefreshTokenException + 새 토큰 발급 안 함")
        void expired() {
            String rawToken = "expired-raw-token";
            RefreshToken expired = expiredToken(UUID.randomUUID(), rawToken);
            given(refreshTokenRepository.findByTokenHash(sha256(rawToken)))
                    .willReturn(Optional.of(expired));

            assertThatThrownBy(() -> refreshTokenService().rotate(rawToken))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("이미 revoke 된 토큰이면 InvalidRefreshTokenException (rotation 으로 한 번 쓰인 토큰 재사용 차단)")
        void alreadyRevoked() {
            String rawToken = "revoked-raw-token";
            RefreshToken revoked = revokedToken(UUID.randomUUID(), rawToken);
            given(refreshTokenRepository.findByTokenHash(sha256(rawToken)))
                    .willReturn(Optional.of(revoked));

            assertThatThrownBy(() -> refreshTokenService().rotate(rawToken))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("revoke — 무효화 (logout)")
    class Revoke {

        @Test
        @DisplayName("존재하는 토큰이면 revokedAt 설정")
        void success() {
            String rawToken = "active-raw-token";
            RefreshToken existing = validToken(UUID.randomUUID(), rawToken);
            given(refreshTokenRepository.findByTokenHash(sha256(rawToken)))
                    .willReturn(Optional.of(existing));

            refreshTokenService().revoke(rawToken);

            assertThat(existing.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 토큰이면 예외 X (idempotent)")
        void notFoundIsIdempotent() {
            given(refreshTokenRepository.findByTokenHash(any()))
                    .willReturn(Optional.empty());

            // 예외 던지지 않아야 함
            refreshTokenService().revoke("ghost");
        }
    }
}
