package hoseo.moodiary.exception;

/**
 * 로그인 실패 (이메일 또는 비밀번호 불일치). {@code 401 Unauthorized}로 매핑된다.
 *
 * <p>이메일 존재 여부를 노출하지 않기 위해, 이메일을 못 찾았든 비번이 틀렸든 동일한 메시지를 쓴다.
 * (열거 공격 방지 — enumeration attack)
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
