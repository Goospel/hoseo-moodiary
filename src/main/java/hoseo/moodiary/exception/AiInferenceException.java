package hoseo.moodiary.exception;

/**
 * AI 추론 외부 호출 실패 시 어댑터 ({@code AiResponseClient}) 가 던지는 런타임 예외.
 *
 * <p>{@code AiResponseService.processAsync()} 가 try/catch 로 잡아
 * {@code AiResponse.markFailed(errorMessage)} 로 DB 에 저장한다. 컨트롤러까지 전파되지 않으므로
 * {@link GlobalExceptionHandler} 에는 매핑하지 않는다 — HTTP 응답으로 노출되지 않음.
 *
 * <p>PR 4-pre (Stub) 단계에선 발생하지 않는다 (Stub 은 항상 성공). PR 4-final 의 실제 HTTP 어댑터에서
 * 타임아웃 / 5xx / 4xx / 응답 파싱 실패 시 던진다.
 */
public class AiInferenceException extends RuntimeException {

    public AiInferenceException(String message) {
        super(message);
    }

    public AiInferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
