package hoseo.moodiary.exception;

/**
 * 이미 가입된 이메일로 회원가입을 시도한 경우. {@code 409 Conflict}로 매핑된다.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("이미 사용 중인 이메일입니다. email=" + email);
    }
}
