package hoseo.moodiary.exception;

/**
 * Refresh token 검증 실패 — 존재하지 않음 / 만료 / 이미 revoke 된 케이스 모두 같은 401.
 *
 * <p>존재 / 만료 / revoke 를 응답으로 구분하지 않는 이유: 정보 노출 최소화. FE 는 어느 쪽이든 재로그인.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("유효하지 않은 refresh token 입니다.");
    }
}
