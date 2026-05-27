package hoseo.moodiary.service;

import hoseo.moodiary.config.JwtProperties;
import hoseo.moodiary.entitiy.RefreshToken;
import hoseo.moodiary.exception.InvalidRefreshTokenException;
import hoseo.moodiary.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh Token 발급 / 회전 (rotation) / 무효화 (revoke).
 *
 * <p><b>저장 정책</b> — raw token 은 클라이언트에만 전달, DB 에는 SHA-256 hex 만 저장.
 * 검증 시 들어온 raw token 을 같은 방식으로 hash 후 비교.
 *
 * <p><b>Rotation</b> — {@link #rotate} 호출 시 기존 토큰 revoke + 새 토큰 발급. 탈취된 refresh 가
 * 한 번만 유효하도록 보장. <b>주의 — race condition</b>: 동시에 두 요청이 같은 refresh 로 들어오면
 * 한 쪽이 무효화되어 사용자 튕김 가능. 졸업 데모 범위에선 발생 확률 낮아 grace window 미적용
 * (필요 시 후속 PR 에서 5초 grace 도입).
 *
 * <p><b>토큰 형식</b> — 32-byte secure random → URL-safe base64 (43자). JWT 가 아닌 이유: refresh 는
 * "이 토큰 ID 가 DB 에 유효한가" 만 필요해서 self-contained payload (JWT) 가 거추장.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 새 refresh 발급 + DB 저장.
     *
     * @return 클라이언트에 전달할 raw token (DB 에는 hash 만)
     */
    public String issue(UUID userId) {
        String rawToken = generateRandomToken();
        String hash = sha256(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plus(jwtProperties.refreshTokenExpirationMs(), ChronoUnit.MILLIS);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .userId(userId)
                        .tokenHash(hash)
                        .expiresAt(expiresAt)
                        .build()
        );
        return rawToken;
    }

    /**
     * 기존 refresh 검증 + 무효화 + 새 발급 (rotation).
     *
     * @throws InvalidRefreshTokenException 존재하지 않음 / 만료 / 이미 revoke 된 경우
     */
    public RotationResult rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!existing.isValid()) {
            throw new InvalidRefreshTokenException();
        }
        existing.revoke();
        String newRawToken = issue(existing.getUserId());
        return new RotationResult(existing.getUserId(), newRawToken);
    }

    /**
     * 명시적 무효화 (logout). 토큰이 없거나 이미 무효화돼도 예외 X — idempotent.
     * 사용자가 로그아웃을 두 번 눌러도 200 / 204 동일.
     */
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** rotate 결과 — userId 와 새 raw token. UserService 가 새 access token 발급에 사용. */
    public record RotationResult(UUID userId, String newRefreshToken) {
    }
}
