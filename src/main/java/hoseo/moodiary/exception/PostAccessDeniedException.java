package hoseo.moodiary.exception;

import java.util.UUID;

/**
 * 본인 소유가 아닌 게시글에 수정/삭제 시도. {@code 403 Forbidden}으로 매핑된다.
 *
 * <p>조회 권한 분리(공개/비공개) 도입 전이라 GET 단건도 동일 예외를 쓴다.
 * 존재 여부 노출을 막으려면 404로 통일하는 선택지도 있지만, 졸업 데모에서는 403이 더 명확.
 */
public class PostAccessDeniedException extends RuntimeException {

    public PostAccessDeniedException(UUID postId) {
        super("게시글에 접근할 권한이 없습니다. id=" + postId);
    }
}
