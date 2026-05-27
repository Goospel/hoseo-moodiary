package hoseo.moodiary.security;

import hoseo.moodiary.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT(HS256) access token 발급/검증.
 *
 * <p>Claims:
 * <ul>
 *   <li>{@code sub} — 사용자 UUID 문자열</li>
 *   <li>{@code iat} — 발급 시각</li>
 *   <li>{@code exp} — 만료 시각</li>
 * </ul>
 *
 * <p><b>책임 분리</b> — 이 클래스는 <b>access token 만</b> 발급/검증한다.
 * Refresh token 은 JWT 가 아니라 random 32-byte token (SHA-256 hash 로 DB 저장) — {@code RefreshTokenService}.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(JwtProperties properties) {
        // jjwt 0.12+ — HS256 키는 최소 32바이트 필요. yaml에 32바이트 미만이 들어오면 여기서 즉시 예외 발생.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = properties.accessTokenExpirationMs();
    }

    /** 사용자 ID로 access token 발급. */
    public String createAccessToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(accessTokenExpirationMs);
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰 파싱 + 검증.
     *
     * @throws JwtException 서명 불일치 / 만료 / 형식 오류 등 (호출 측에서 401로 매핑)
     */
    public UUID getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }
}
