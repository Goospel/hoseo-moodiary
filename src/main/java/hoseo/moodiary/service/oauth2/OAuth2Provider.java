package hoseo.moodiary.service.oauth2;

import hoseo.moodiary.entitiy.AuthProvider;

/**
 * 외부 OAuth2 provider 추상화.
 *
 * <p>provider 별 구현 (Google {@code tokeninfo} / Kakao {@code /v2/user/me}) 의 차이를 흡수.
 *
 * <p>PR 12-pre 단계에서는 {@link StubOAuth2Provider} 단일 구현. PR 12-final 단계에서 실 HTTP 어댑터
 * ({@code HttpGoogleOAuth2Provider} / {@code HttpKakaoOAuth2Provider}) 로 교체 / 추가.
 *
 * <p><b>토글 패턴 — `oauth2.client.mode`</b>:
 * 운영에선 `http` (실 어댑터), 통합 테스트 / Stub 환경에선 `stub`. (PR 12-final 단계에서 도입.)
 */
public interface OAuth2Provider {

    /**
     * 이 구현이 담당하는 provider.
     */
    AuthProvider provider();

    /**
     * Provider 측 access token 을 검증하고 사용자 정보 추출.
     *
     * <p>실 구현 (PR 12-final):
     * <ul>
     *   <li>Google — {@code https://oauth2.googleapis.com/tokeninfo?access_token=<token>} GET. 응답의 {@code aud} 가
     *       우리 client_id 와 일치하는지 검증 필수 (token confused deputy 공격 방어).</li>
     *   <li>Kakao — {@code https://kapi.kakao.com/v2/user/me} GET + {@code Authorization: Bearer <token>}.</li>
     * </ul>
     *
     * @param providerAccessToken provider 가 FE 에게 발급한 access token
     * @return 검증을 통과한 user info
     * @throws OAuth2VerificationException 토큰 무효 / provider 측 거절 / audience 불일치 등
     */
    OAuth2UserInfo verifyAndExtract(String providerAccessToken);
}
