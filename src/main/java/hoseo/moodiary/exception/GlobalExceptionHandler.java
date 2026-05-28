package hoseo.moodiary.exception;

import hoseo.moodiary.dto.response.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlePostNotFound(PostNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler({DuplicateEmailException.class, DuplicateNicknameException.class})
    public ResponseEntity<ErrorResponseDto> handleDuplicate(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(OAuth2VerificationException.class)
    public ResponseEntity<ErrorResponseDto> handleOAuth2Verification(OAuth2VerificationException e) {
        // 사유 노출 최소 — provider 내부 구조 / 토큰 형식 단서를 응답에 박지 않는다.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDto.builder().message("OAuth2 인증 실패").build());
    }

    /**
     * OAuth2 path 의 provider 가 미지원 enum — 400.
     *
     * <p>T-031 — Spring 기본 String→Enum 변환은 case-sensitive 라 {@code /auth/oauth2/google} (소문자) 가
     * 변환 실패해 generic 500 으로 떨어지던 사고의 fix. controller 가 명시적으로 uppercase 정규화 후
     * 미지원 값일 때만 이 예외를 던진다.
     */
    @ExceptionHandler(InvalidOAuth2ProviderException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidOAuth2Provider(InvalidOAuth2ProviderException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(EmailAlreadyExistsForOtherProviderException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailDifferentProvider(EmailAlreadyExistsForOtherProviderException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(PostAccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handlePostAccessDenied(PostAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(AiResponseNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleAiResponseNotFound(AiResponseNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(CalendarInvalidRangeException.class)
    public ResponseEntity<ErrorResponseDto> handleCalendarInvalidRange(CalendarInvalidRangeException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponseDto.builder().message(e.getMessage()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest()
                .body(ErrorResponseDto.builder().message(message).build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponseDto.builder().message("요청 형식이 올바르지 않습니다.").build());
    }

    /**
     * 필수 쿼리 파라미터 누락 — 예: {@code GET /calendar} 호출 시 year/month 없음.
     * 명시적으로 400 으로 매핑하지 않으면 fallback {@code Exception} 핸들러가 잡아 500 으로 떨어진다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponseDto.builder()
                        .message("필수 파라미터가 누락되었습니다: " + e.getParameterName())
                        .build());
    }

    /**
     * 매핑되지 않은 모든 예외의 최종 안전망 — 500.
     *
     * <p>T-032 — 이전엔 {@code e} 를 무시한 채 generic 500 만 돌려줬다. 그래서 운영에서 case-sensitive
     * path enum 변환 실패 (T-031) 같은 함정이 docker logs 에 흔적조차 안 남기고 묻혀 진단이 불가능했다.
     * 이 핸들러가 마지막 catch 라서, 잡힌 예외는 명시적 핸들러를 통과 못 한 미분류 상태다 — 향후 분기
     * 추가의 단서가 되도록 stack trace 를 ERROR 로 떨어뜨린다.
     *
     * <p>응답 body 는 그대로 (사용자 노출 정보 변경 없음).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception e) {
        log.error("Unhandled exception reached generic 500 — 명시적 @ExceptionHandler 분기 추가 후보", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.builder().message("서버 오류가 발생했습니다.").build());
    }
}
