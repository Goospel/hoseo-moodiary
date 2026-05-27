package hoseo.moodiary.security;

import hoseo.moodiary.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트.
 *
 * <p>Spring 컨텍스트 없이 직접 인스턴스화하여 발급/검증 라운드트립 확인.
 *
 * <p>PR 10 이후 — JwtProperties 가 access + refresh 만료 시간을 따로 들고 있다.
 * 이 테스트는 access 만 검증. refresh 는 별도 클래스 (RefreshTokenService) 책임.
 */
class JwtTokenProviderTest {

    /** HS256 키는 최소 32바이트 필요. */
    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key";
    private static final long ACCESS_EXPIRATION_MS = 60_000; // 1분
    private static final long REFRESH_EXPIRATION_MS = 600_000; // 10분 (이 테스트엔 영향 X — refresh 는 다른 클래스 책임)

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(new JwtProperties(SECRET, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS));
    }

    @Test
    @DisplayName("발급 → 파싱 라운드트립: 동일한 userId가 복원된다")
    void roundTrip() {
        UUID userId = UUID.randomUUID();

        String token = provider.createAccessToken(userId);
        UUID parsed = provider.getUserId(token);

        assertThat(token).isNotBlank();
        assertThat(parsed).isEqualTo(userId);
    }

    @Test
    @DisplayName("다른 키로 발급된 토큰은 검증 실패한다 (서명 불일치)")
    void rejectsTokenSignedWithDifferentKey() {
        UUID userId = UUID.randomUUID();
        JwtTokenProvider other = new JwtTokenProvider(
                new JwtProperties("different-key-different-key-different-key!", ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS));
        String foreignToken = other.createAccessToken(userId);

        assertThatThrownBy(() -> provider.getUserId(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("형식이 깨진 토큰은 검증 실패한다")
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> provider.getUserId("not.a.jwt"))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이미 만료된 토큰은 검증 실패한다 (exp가 과거)")
    void rejectsExpiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(new JwtProperties(SECRET, -1_000, REFRESH_EXPIRATION_MS));
        String expired = expiredProvider.createAccessToken(UUID.randomUUID());

        assertThatThrownBy(() -> provider.getUserId(expired))
                .isInstanceOf(JwtException.class);
    }
}
