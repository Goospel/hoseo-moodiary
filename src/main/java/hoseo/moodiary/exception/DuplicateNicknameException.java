package hoseo.moodiary.exception;

/**
 * 이미 사용 중인 닉네임으로 회원가입을 시도한 경우. {@code 409 Conflict}로 매핑된다.
 */
public class DuplicateNicknameException extends RuntimeException {

    public DuplicateNicknameException(String nickname) {
        super("이미 사용 중인 닉네임입니다. nickname=" + nickname);
    }
}
