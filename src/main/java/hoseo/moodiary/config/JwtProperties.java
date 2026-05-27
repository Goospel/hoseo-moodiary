package hoseo.moodiary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정 바인딩.
 *
 * <p>application.yaml 의 {@code jwt.*} 키에서 읽는다.
 * 운영에서는 환경변수로 override — {@code JWT_SECRET}, {@code JWT_ACCESS_EXPIRATION_MS},
 * {@code JWT_REFRESH_EXPIRATION_MS}.
 *
 * <p><b>secret</b> — HS256 서명 키. 최소 32바이트(256-bit) 이상이어야 jjwt 가 거부하지 않는다.
 * <br><b>accessTokenExpirationMs</b> — access token 수명 (기본 1시간 = 3_600_000ms).
 *                                       만료 시 클라이언트가 {@code POST /auth/refresh} 로 갱신.
 * <br><b>refreshTokenExpirationMs</b> — refresh token 수명 (기본 2주 = 1_209_600_000ms).
 *                                        만료 시 재로그인 필요.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs
) {
}
