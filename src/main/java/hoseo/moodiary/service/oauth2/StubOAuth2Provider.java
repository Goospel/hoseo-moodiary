package hoseo.moodiary.service.oauth2;

import hoseo.moodiary.entitiy.AuthProvider;
import hoseo.moodiary.exception.OAuth2VerificationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stub OAuth2 Provider — 외부 Console 셋업 / HTTP 호출 없이 OAuth2 흐름을 단독으로 진행 가능하게 한다.
 *
 * <p>활성화 조건: {@code oauth2.client.mode=stub} (기본값). 운영 / 시연에선 {@code http} 로 전환되어 비활성.
 *
 * <p><b>토큰 형식 — {@code stub:{provider}:{providerId}:{email}:{nickname}}</b>:
 * <ul>
 *   <li>예: {@code stub:GOOGLE:google-123:alice@example.com:Alice}</li>
 * </ul>
 *
 * <p>형식이 깨지거나 prefix 가 {@code stub:} 가 아니면 {@link OAuth2VerificationException} (401).
 *
 * <p>{@link #provider()} 의 반환값은 의미상 placeholder — 현재 Stub 은 단일 인스턴스로 모든 provider 의 path 호출을
 * 처리한다. 다른 provider 부활 + {@code OAuth2ProviderRegistry} 도입 시점에 의미 명확해짐.
 */
@Component
@ConditionalOnProperty(name = "oauth2.client.mode", havingValue = "stub", matchIfMissing = true)
public class StubOAuth2Provider implements OAuth2Provider {

    private static final String STUB_PREFIX = "stub:";

    /**
     * Stub 은 모든 provider 를 처리 — placeholder 로 GOOGLE 반환.
     */
    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
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
