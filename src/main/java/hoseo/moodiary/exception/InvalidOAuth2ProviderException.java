package hoseo.moodiary.exception;

/**
 * OAuth2 path 의 provider 가 지원 enum 아닌 경우.
 *
 * <p>예: {@code POST /auth/oauth2/twitter} — Twitter 는 미지원. 400 응답.
 *
 * <p>주의: 운영 사고로 발견된 함정 — Spring 의 기본 {@code String→Enum} 변환은 **case-sensitive**.
 * FE 가 REST convention 대로 {@code /auth/oauth2/google} (소문자) 보내면 enum {@code GOOGLE} 로 변환 실패 →
 * 이전엔 {@code MethodArgumentTypeMismatchException} 으로 갈 길 잃고 generic 500 으로 떨어졌다 (T-031).
 * 이 예외는 controller 에서 명시적 uppercase 정규화 + 미지원 값 검출 후 던진다.
 */
public class InvalidOAuth2ProviderException extends RuntimeException {

    public InvalidOAuth2ProviderException(String providerName) {
        super("지원하지 않는 OAuth2 provider: " + providerName);
    }
}
