package hoseo.moodiary.exception;

/**
 * 캘린더 조회 시 year/month 범위 위반. {@link GlobalExceptionHandler}에서 400 으로 매핑.
 */
public class CalendarInvalidRangeException extends RuntimeException {

    public CalendarInvalidRangeException(String message) {
        super(message);
    }
}
