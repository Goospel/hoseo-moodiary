package hoseo.moodiary.service.ai;

/**
 * AI 추론 어댑터의 성공 응답.
 *
 * <p>{@code content} 는 사용자 일기에 대한 AI 응답 본문, {@code emoji} 는 기분을 나타내는 유니코드 이모지 1자.
 * 실패는 {@code hoseo.moodiary.exception.AiInferenceException} 으로 던지므로 별도 실패 표현 필드 없음.
 */
public record AiInferenceResult(String content, String emoji) {
}
