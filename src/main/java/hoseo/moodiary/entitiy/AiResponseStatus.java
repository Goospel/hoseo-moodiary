package hoseo.moodiary.entitiy;

/**
 * AI 응답의 진행 상태.
 *
 * <p>전이: {@code PENDING → DONE} 또는 {@code PENDING → FAILED}. 재시도/회귀는 없다 (1:1 모델).
 */
public enum AiResponseStatus {
    /** 일기 저장 직후 row 가 만들어진 상태. Async 워커 진입 전 또는 외부 호출 진행 중. */
    PENDING,
    /** 외부 추론 성공 — {@code content} 와 {@code emoji} 가 채워짐. */
    DONE,
    /** 외부 추론 실패 — {@code errorMessage} 에 사유. {@code content / emoji} 는 null. */
    FAILED
}
