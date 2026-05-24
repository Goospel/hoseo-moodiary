package hoseo.moodiary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정 바인딩.
 *
 * <p>{@code jwt.secret}, {@code jwt.expiration-ms}를 application.yaml에서 읽는다.
 * 운영에서는 {@code JWT_SECRET} 환경변수로 override한다.
 *
 * <p><b>secret</b> — HS256 서명 키. 최소 32바이트(256-bit) 이상이어야 jjwt가 거부하지 않는다.
 * <br><b>expirationMs</b> — access token 수명(밀리초). 기본 24시간(86,400,000ms).
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMs) {
}
