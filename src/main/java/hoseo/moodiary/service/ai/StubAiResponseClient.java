package hoseo.moodiary.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AI 추론 어댑터 — Stub 구현 (외부 호출 없이 고정 응답).
 *
 * <p>활성화 조건: {@code ai.client.mode=stub} (기본값). 운영/시연에서 AI 서버가 준비되면
 * {@code AI_CLIENT_MODE=http} 로 전환되어 비활성.
 *
 * <p>골격 PR(4-pre)의 end-to-end (Async 디스패치 → DB 상태 전이 → 폴링 응답) 검증용. 사용자 일기 내용과
 * 무관하게 같은 응답을 즉시 반환하고 항상 성공(DONE)한다.
 */
@Component
@ConditionalOnProperty(name = "ai.client.mode", havingValue = "stub", matchIfMissing = true)
public class StubAiResponseClient implements AiResponseClient {

    private static final String STUB_CONTENT = "(stub) AI 응답은 AI 서버 연동(ai.client.mode=http) 후 활성화됩니다.";
    private static final String STUB_EMOJI = "😊";

    @Override
    public AiInferenceResult invoke(UUID userId, UUID postId, String title, String content) {
        return new AiInferenceResult(STUB_CONTENT, STUB_EMOJI);
    }
}
