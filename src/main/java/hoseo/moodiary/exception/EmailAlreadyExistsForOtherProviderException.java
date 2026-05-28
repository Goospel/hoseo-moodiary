package hoseo.moodiary.exception;

import hoseo.moodiary.entitiy.AuthProvider;

/**
 * OAuth2 신규 가입 시도 — 그러나 동일 이메일이 다른 provider 로 이미 가입되어 있는 경우.
 *
 * <p>예: 사용자가 이미 LOCAL 로 가입한 이메일을 Google 로 다시 가입하려 시도.
 * 정책 (plan.md 의사결정 로그 — PR 12-pre 합의): **LOCAL + OAuth2 양쪽 가입 차단** — 같은 이메일은 한 provider 만.
 *
 * <p>이유:
 * <ul>
 *   <li>이메일 = 사용자의 identity 단일 키 — 다중 provider 허용하면 account fragmentation</li>
 *   <li>{@code uk_users_email} UNIQUE 제약과 일관</li>
 *   <li>구현 단순 — auto-link 흐름의 보안 함정 (Provider 별 verified 정도 차이) 회피</li>
 * </ul>
 *
 * <p>응답: 409 Conflict + 어느 provider 로 가입돼 있는지 안내 (사용자가 원래 가입 경로로 로그인하도록).
 */
public class EmailAlreadyExistsForOtherProviderException extends RuntimeException {

    private final AuthProvider existingProvider;

    public EmailAlreadyExistsForOtherProviderException(String email, AuthProvider existingProvider) {
        super("이메일 " + email + " 은 이미 " + existingProvider + " 로 가입되어 있습니다.");
        this.existingProvider = existingProvider;
    }

    public AuthProvider getExistingProvider() {
        return existingProvider;
    }
}
