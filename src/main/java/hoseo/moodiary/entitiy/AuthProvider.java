package hoseo.moodiary.entitiy;

/**
 * 인증 제공자.
 *
 * <p>회원의 가입 / 로그인 경로를 구분한다.
 * <ul>
 *   <li>{@link #LOCAL} — 자체 회원가입 (이메일 + 비밀번호). {@code password} 필수, {@code providerId} null.</li>
 *   <li>{@link #GOOGLE} — Google OAuth2. {@code password} null, {@code providerId} = Google 의 {@code sub} 클레임.</li>
 * </ul>
 *
 * <p>DB 에는 {@code @Enumerated(EnumType.STRING)} 으로 문자열 저장 — enum 순서 변경에 강함.
 *
 * <p><b>Kakao 보류 — PR 12-final</b>: 졸업프로젝트 범위에서 외부 Console 마찰 (사이트 도메인이 localhost 거부 →
 * FE 배포 선행 요구) 이 큰 데 비해 학습 가치가 Google 과 거의 동일해서 빠짐.
 * 부활 비용은 작음: (1) 이 enum 에 {@code KAKAO} 추가, (2) {@code HttpKakaoOAuth2Provider} 구현,
 * (3) {@link hoseo.moodiary.service.oauth2.OAuth2ProviderRegistry} 도입 (현재는 단일 bean — registry 불필요).
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
