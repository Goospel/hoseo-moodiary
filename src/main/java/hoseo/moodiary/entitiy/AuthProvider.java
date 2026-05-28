package hoseo.moodiary.entitiy;

/**
 * 인증 제공자.
 *
 * <p>회원의 가입 / 로그인 경로를 구분한다.
 * <ul>
 *   <li>{@link #LOCAL} — 자체 회원가입 (이메일 + 비밀번호). {@code password} 필수, {@code providerId} null.</li>
 *   <li>{@link #GOOGLE} — Google OAuth2. {@code password} null, {@code providerId} = Google 의 {@code sub} 클레임.</li>
 *   <li>{@link #KAKAO} — Kakao OAuth2. {@code password} null, {@code providerId} = Kakao 의 {@code id} (Long → String).</li>
 * </ul>
 *
 * <p>DB 에는 {@code @Enumerated(EnumType.STRING)} 으로 문자열 저장 — enum 순서 변경에 강함.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    KAKAO
}
