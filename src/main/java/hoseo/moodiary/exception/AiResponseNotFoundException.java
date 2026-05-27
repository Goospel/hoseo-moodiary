package hoseo.moodiary.exception;

import java.util.UUID;

/**
 * 주어진 {@code postId} 에 대한 AI 응답이 없을 때. {@code 404 Not Found} 로 매핑된다 ({@link GlobalExceptionHandler}).
 *
 * <p>두 케이스를 한 예외로 통합:
 * <ul>
 *   <li>{@code postId} 자체가 존재하지 않음</li>
 *   <li>post 는 있지만 AI 응답이 트리거되지 않음 (이론상 발생 X — POST /post 가 PENDING row 를 함께 만들기 때문)</li>
 * </ul>
 *
 * <p>본인 글이 아닌 경우는 별도 {@link PostAccessDeniedException} 으로 403 이 먼저 떨어진다 —
 * post 존재 여부 노출 방지를 위해 권한 검증을 lookup 보다 우선.
 */
public class AiResponseNotFoundException extends RuntimeException {

    public AiResponseNotFoundException(UUID postId) {
        super("AI 응답을 찾을 수 없습니다. postId=" + postId);
    }
}
