package hoseo.moodiary.service.ai;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AI 추론 어댑터 — <b>PR 4-pre 의 Stub 구현</b>.
 *
 * <p><b>현재 (Stub)</b>: 외부 호출 없이 고정 응답을 즉시 반환. 골격 PR 의 end-to-end
 * (Async 디스패치 → DB 상태 전이 → 폴링 응답) 검증용. 사용자 일기 내용과 무관하게 같은 응답.
 *
 * <p><b>PR 4-final</b>: 이 클래스를 interface 로 추출하고 {@code HttpAiResponseClient} 구현체를 추가.
 * 그 시점에 외부 AI 서버 호출 + Retry + 타임아웃 정책을 도입. 본 클래스는 fallback / 테스트용으로 유지.
 *
 * <p>실패 표현: 향후 실 어댑터는 타임아웃 / 5xx / 4xx / 파싱 실패 시
 * {@link hoseo.moodiary.exception.AiInferenceException} 을 던진다. Stub 은 항상 성공.
 */
@Component
public class AiResponseClient {

    private static final String STUB_CONTENT = "(stub) AI 응답은 PR 4-final 머지 후 활성화됩니다.";
    private static final String STUB_EMOJI = "😊";

    public AiInferenceResult invoke(UUID postId, String title, String content) {
        return new AiInferenceResult(STUB_CONTENT, STUB_EMOJI);
    }
}
