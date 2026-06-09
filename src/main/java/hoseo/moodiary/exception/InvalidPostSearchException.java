package hoseo.moodiary.exception;

/**
 * 게시글 목록 조회의 검색/정렬 파라미터가 잘못된 경우 — 400.
 *
 * <p>커버 케이스:
 * <ul>
 *   <li>화이트리스트에 없는 정렬 필드 (예: {@code sort=content})</li>
 *   <li>알 수 없는 정렬 방향 (예: {@code sort=postDate,sideways})</li>
 *   <li>날짜 범위 역전 ({@code from} 이 {@code to} 보다 늦음)</li>
 * </ul>
 *
 * <p>{@code CalendarInvalidRangeException} 과 같은 400 카테고리지만, 캘린더와 책임을 분리해 둔다.
 */
public class InvalidPostSearchException extends RuntimeException {

    public InvalidPostSearchException(String message) {
        super(message);
    }
}
