package hoseo.moodiary.service.oauth2;

import hoseo.moodiary.entitiy.AuthProvider;
import hoseo.moodiary.exception.OAuth2VerificationException;
import org.springframework.stereotype.Component;

/**
 * PR 12-pre 단계의 Stub OAuth2 Provider — 외부 Console 셋업 / HTTP 호출 없이 OAuth2 흐름을 단독으로 진행 가능하게 한다.
 *
 * <p><b>토큰 형식 — `stub:{provider}:{providerId}:{email}:{nickname}`</b>:
 * <ul>
 *   <li>예: {@code stub:GOOGLE:google-123:alice@example.com:Alice}</li>
 *   <li>예: {@code stub:KAKAO:kakao-456:bob@example.com:Bob}</li>
 * </ul>
 *
 * <p>형식이 깨지거나 prefix 가 {@code stub:} 가 아니면 {@link OAuth2VerificationException} (401).
 *
 * <p>이 Stub 은 단일 구현으로 GOOGLE / KAKAO 양쪽을 처리한다 — {@link #provider()} 는 의미상 사용 안 됨 (controller 에서 path 의 provider 사용).
 * PR 12-final 단계에서는 provider 별 실 HTTP 어댑터 (HttpGoogleOAuth2Provider / HttpKakaoOAuth2Provider) 가 등장하며
 * {@code OAuth2ProviderRegistry} 로 dispatch.
 *
 * <p>PR 12-final 머지 후 이 Stub 은 통합 테스트 / 로컬 개발 용으로만 남기고 운영에선 토글 (`oauth2.client.mode=http`) 로 비활성화.
 */
@Component
public class StubOAuth2Provider implements OAuth2Provider {

    private static final String STUB_PREFIX = "stub:";

    /**
     * Stub 은 의미상 어떤 provider 도 처리 가능 — registry 의 default fallback 으로 등록되거나
     * 모든 provider 에 대해 같은 Stub 인스턴스를 매핑. 실 어댑터 도입 시 의미 명확해짐.
     */
    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE; // placeholder — PR 12-final 에서 registry 도입 후 의미 정확해짐
    }

    @Override
    public OAuth2UserInfo verifyAndExtract(String providerAccessToken) {
        if (providerAccessToken == null || !providerAccessToken.startsWith(STUB_PREFIX)) {
            throw new OAuth2VerificationException("Stub 토큰 형식 아님 — prefix `stub:` 필요");
        }

        // stub:{provider}:{providerId}:{email}:{nickname}
        String payload = providerAccessToken.substring(STUB_PREFIX.length());
        String[] parts = payload.split(":");
        if (parts.length != 4) {
            throw new OAuth2VerificationException("Stub 토큰 형식 오류 — `stub:{provider}:{providerId}:{email}:{nickname}` 4 segments 필요");
        }

        // parts[0] = provider 이름 — controller 의 path 와 일관성 검증은 OAuth2Service 가 담당
        String providerId = parts[1];
        String email = parts[2];
        String nickname = parts[3];

        if (providerId.isBlank() || email.isBlank() || nickname.isBlank()) {
            throw new OAuth2VerificationException("Stub 토큰의 providerId / email / nickname 빈 값");
        }

        return new OAuth2UserInfo(providerId, email, nickname);
    }
}
