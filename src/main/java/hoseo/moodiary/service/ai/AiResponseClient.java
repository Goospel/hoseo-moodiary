package hoseo.moodiary.service.ai;

import java.util.UUID;

/**
 * AI 추론 어댑터 — 일기 한 건을 AI 서버로 보내 응답 메시지 + 이모지를 받는다.
 *
 * <p><b>구현 토글</b> ({@code ai.client.mode}):
 * <ul>
 *   <li>{@code stub} (기본) — {@link StubAiResponseClient}. 외부 호출 없이 고정 응답.</li>
 *   <li>{@code http} — {@link HttpAiResponseClient}. 실제 AI 서버 호출.</li>
 * </ul>
 * OAuth2 의 {@code oauth2.client.mode} 와 같은 패턴 — 외부 의존성(AI 서버 배포)이 없어도 부팅/테스트 가능.
 *
 * <p><b>실패 표현</b>: 타임아웃 / 5xx / 4xx / 파싱 실패 시
 * {@link hoseo.moodiary.exception.AiInferenceException} 을 던진다. 성공 응답만 {@link AiInferenceResult} 로 반환.
 * 호출자({@code AiResponseService.triggerAsync})가 이 예외를 잡아 DB 의 FAILED 상태로 전이 — 사용자의
 * 일기 작성 흐름은 절대 막지 않는다.
 */
public interface AiResponseClient {

    /**
     * @param userId  작성자 식별 UUID (AI 서버가 사용자 단위 컨텍스트/식별에 사용)
     * @param postId  게시글 식별 UUID
     * @param title   일기 제목
     * @param content 일기 내용
     * @return AI 가 도출한 메시지 + 이모지
     * @throws hoseo.moodiary.exception.AiInferenceException 타임아웃 / HTTP 4xx·5xx / 파싱 실패
     */
    AiInferenceResult invoke(UUID userId, UUID postId, String title, String content);
}
