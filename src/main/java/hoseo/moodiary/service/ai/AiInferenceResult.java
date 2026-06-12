package hoseo.moodiary.service.ai;

/**
 * AI 추론 어댑터의 성공 응답.
 *
 * <p>AI 서버({@code POST /chat})의 응답 3필드를 우리 도메인으로 옮긴 값:
 * <ul>
 *   <li>{@code content} — 사용자 일기에 대한 AI 공감/분석 문장 (서버 응답 {@code aiText}). DB {@code ai_response_content} 로 매핑.</li>
 *   <li>{@code emotion} — 감정 라벨 문자열 (예: {@code "neutral"}). DB {@code ai_response_emotion} 으로 매핑.</li>
 *   <li>{@code homeComment} — 홈화면에 보여줄 짧은 문장. DB {@code ai_response_home_comment} 로 매핑.</li>
 * </ul>
 *
 * <p>실패는 {@code hoseo.moodiary.exception.AiInferenceException} 으로 던지므로 별도 실패 표현 필드 없음.
 */
public record AiInferenceResult(String content, String emotion, String homeComment) {
}
