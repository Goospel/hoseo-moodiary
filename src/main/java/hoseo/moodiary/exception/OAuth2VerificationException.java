package hoseo.moodiary.exception;

/**
 * OAuth2 provider 측 access token 검증 실패.
 *
 * <p>발생 케이스:
 * <ul>
 *   <li>Provider 가 토큰을 reject (만료 / invalid signature / revoked)</li>
 *   <li>Google {@code aud} 가 우리 client_id 와 불일치 (token confused deputy)</li>
 *   <li>Provider 측 HTTP 호출 실패 (5xx / timeout) — PR 12-final 단계에서 retry 정책</li>
 * </ul>
 *
 * <p>응답: 401 Unauthorized. 자세한 사유는 노출하지 않음 (열거 공격 / provider 내부 구조 노출 방지).
 */
public class OAuth2VerificationException extends RuntimeException {

    public OAuth2VerificationException(String message) {
        super(message);
    }

    public OAuth2VerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
