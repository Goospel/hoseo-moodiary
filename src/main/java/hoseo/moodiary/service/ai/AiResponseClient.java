package hoseo.moodiary.service.ai;

import java.time.LocalDate;
import java.util.UUID;

/**
 * AI 추론 어댑터 — 일기 한 건을 AI 서버로 보내 공감 문장 + 감정 라벨 + 홈 코멘트를 받는다.
 *
 * <p><b>구현 토글</b> ({@code ai.client.mode}):
 * <ul>
 *   <li>{@code stub} (기본) — {@link StubAiResponseClient}. 외부 호출 없이 고정 응답.</li>
 *   <li>{@code http} — {@link HttpAiResponseClient}. 실제 AI 서버({@code POST /chat}) 호출.</li>
 * </ul>
 * OAuth2 의 {@code oauth2.client.mode} 와 같은 패턴 — 외부 의존성(AI 서버 배포)이 없어도 부팅/테스트 가능.
 *
 * <p><b>실패 표현</b>: 타임아웃 / 5xx / 4xx / 파싱 실패 시
 * {@link hoseo.moodiary.exception.AiInferenceException} 을 던진다. 성공 응답만 {@link AiInferenceResult} 로 반환.
 * 호출자({@code AiResponseService.triggerAsync})가 이 예외를 잡아 DB 의 FAILED 상태로 전이 — 사용자의
 * 일기 작성 흐름은 절대 막지 않는다.
 *
 * <p><b>계약 메모</b>: AI 서버는 {@code recent_emotions}(최근 감정 요약) 입력을 받지만, 현 단계에선
 * 항상 빈 문자열로 보낸다 (AI 담당자 합의 — 미사용). 그래서 이 시그니처엔 그 파라미터가 없다.
 */
public interface AiResponseClient {

    /**
     * @param postId    게시글 식별 UUID (로그/예외 메시지용 — 요청 본문에는 실리지 않음)
     * @param content   일기 내용 (AI 서버 {@code user_text}). 빈 문자열 금지 — 호출 전 {@code @NotBlank} 로 보장됨.
     * @param diaryDate 일기 날짜 (AI 서버 {@code diary_date} — {@code "M월 d일"} 한국어 포맷으로 변환되어 전송)
     * @return AI 가 도출한 공감 문장 + 감정 라벨 + 홈 코멘트
     * @throws hoseo.moodiary.exception.AiInferenceException 타임아웃 / HTTP 4xx·5xx / 파싱 실패
     */
    AiInferenceResult invoke(UUID postId, String content, LocalDate diaryDate);
}
