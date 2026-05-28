package hoseo.moodiary.service.oauth2;

import hoseo.moodiary.entitiy.AuthProvider;

/**
 * 외부 OAuth2 provider 추상화.
 *
 * <p>provider 별 구현의 차이를 흡수. 현재 졸업프로젝트 범위는 Google 만 지원 — Kakao 는
 * {@link AuthProvider} Javadoc 의 보류 사유 참조.
 *
 * <p>구현체:
 * <ul>
 *   <li>{@link StubOAuth2Provider} — Stub 모드 ({@code oauth2.client.mode=stub}). 외부 HTTP 없이 단독 테스트 가능.</li>
 *   <li>{@code HttpGoogleOAuth2Provider} — HTTP 모드 ({@code oauth2.client.mode=http}). 실제 Google tokeninfo 호출.</li>
 * </ul>
 *
 * <p><b>토글 — {@code oauth2.client.mode}</b>:
 * 운영 / 통합 시연에선 {@code http}, 로컬 dev / 단위 테스트에선 {@code stub}.
 * application.yaml 기본값은 {@code stub} — 외부 키 없어도 부팅됨.
 *
 * <p><b>다중 provider 부활 시</b>: 같은 {@code http} 모드 안에서 여러 {@code Http*OAuth2Provider} 가 등장하면
 * {@code OAuth2ProviderRegistry} 도입 + path 의 provider 로 dispatch. 현재 단일 구현이라 불필요.
 */
public interface OAuth2Provider {

    /**
     * 이 구현이 담당하는 provider.
     */
    AuthProvider provider();

    /**
     * Provider 측 token 을 검증하고 사용자 정보 추출.
     *
     * <p>Google 실 구현:
     * {@code https://oauth2.googleapis.com/tokeninfo?id_token=<token>} GET. 응답의:
     * <ul>
     *   <li>{@code aud} 가 우리 {@code GOOGLE_OAUTH_CLIENT_ID} 와 일치 — token confused deputy 공격 방어.</li>
     *   <li>{@code email_verified=="true"} — Google 측 이메일 검증된 계정만 허용.</li>
     *   <li>{@code sub} / {@code email} / {@code name} 추출.</li>
     * </ul>
     *
     * @param providerAccessToken provider 가 FE 에게 발급한 token (Google: id_token 권장, access_token 도 호환)
     * @return 검증을 통과한 user info
     * @throws OAuth2VerificationException 토큰 무효 / provider 측 거절 / audience 불일치 / email 미검증
     */
    OAuth2UserInfo verifyAndExtract(String providerAccessToken);
}
