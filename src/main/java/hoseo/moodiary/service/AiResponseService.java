package hoseo.moodiary.service;

import hoseo.moodiary.dto.response.AiResponseDto;
import hoseo.moodiary.entitiy.AiResponse;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.exception.AiInferenceException;
import hoseo.moodiary.exception.AiResponseNotFoundException;
import hoseo.moodiary.exception.PostAccessDeniedException;
import hoseo.moodiary.repository.AiResponseJpaRepository;
import hoseo.moodiary.repository.PostJpaRepository;
import hoseo.moodiary.service.ai.AiInferenceResult;
import hoseo.moodiary.service.ai.AiResponseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * AI 응답 비즈니스 로직.
 *
 * <p>책임 두 가지:
 * <ul>
 *   <li>{@link #triggerAsync(UUID)} — 비동기 추론 디스패치. 컨트롤러가 일기 저장 트랜잭션 commit 후 호출한다.</li>
 *   <li>{@link #getByPostId(UUID, UUID)} — 폴링. 본인 글 소유권 검증 후 현재 상태/내용 반환.</li>
 * </ul>
 *
 * <p>PENDING row 의 *생성* 은 {@code PostService.create()} 가 일기 저장과 같은 트랜잭션에서 직접 처리한다
 * (정합성 보장 — 일기는 저장됐는데 PENDING row 가 없는 상태가 발생하지 않도록).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiResponseService {

    private final AiResponseJpaRepository aiResponseRepository;
    private final PostJpaRepository postRepository;
    private final AiResponseClient client;

    /**
     * 비동기 추론. 별도 스레드 + 자기 트랜잭션에서 실행.
     *
     * <p><b>호출자 의무</b>: 일기 저장 트랜잭션 commit 후에 호출해야 한다. 이 메서드의 새 트랜잭션이
     * PENDING row 를 select 할 수 있어야 하기 때문 — commit 전 호출 시 race condition.
     *
     * <p>실패는 호출자에 전파되지 않는다 ({@code @Async void}). 내부 try/catch 로 잡아
     * {@link AiResponse#markFailed(String)} 으로 DB 의 FAILED 상태로만 표현 — 사용자의 일기 작성 흐름은
     * 절대 막지 않는다는 정책에 따름.
     *
     * <p>Stub (PR 4-pre) 단계에선 항상 성공 → DONE. PR 4-final 의 실 어댑터는 타임아웃 / 5xx / 4xx /
     * 파싱 실패 시 {@link AiInferenceException} 을 던져 FAILED 분기로 가게 한다.
     */
    @Async
    public void triggerAsync(UUID postId) {
        AiResponse aiResponse = aiResponseRepository.findByPost_Id(postId)
                .orElseThrow(() -> new AiResponseNotFoundException(postId));
        Post post = aiResponse.getPost();
        // user 는 LAZY 프록시 — getId() 는 식별자만 읽어 추가 쿼리 없이 동작 (isOwnedBy 와 같은 패턴).
        UUID userId = post.getUser().getId();
        try {
            AiInferenceResult result = client.invoke(userId, postId, post.getTitle(), post.getContent());
            aiResponse.markDone(result.content(), result.emoji());
        } catch (AiInferenceException e) {
            // DB 의 FAILED 상태 + error_message 가 1차 진단 채널이지만 — 로그도 같이 떨어뜨려서
            // 운영 grep 만으로 "최근 1시간 AI 실패 건수" 가 보이게 한다 (T-033).
            // WARN 레벨: 예상된 외부 fault — 우리 코드 버그 아니라 ERROR 아님.
            log.warn("AI inference failed for postId={} — DB FAILED 상태로 마킹", postId, e);
            aiResponse.markFailed(e.getMessage());
        }
        // dirty checking — 트랜잭션 commit 시점에 update.
    }

    /**
     * 폴링 조회. 본인 글이 아니면 {@link PostAccessDeniedException} (403) 이 먼저 떨어진다 —
     * post 존재 여부 노출 방지.
     *
     * <p>404 두 케이스 ({@code postId} 미존재 / AI 응답 row 미존재) 모두
     * {@link AiResponseNotFoundException} 으로 통일. api-contracts 의 정책과 일관.
     */
    @Transactional(readOnly = true)
    public AiResponseDto getByPostId(UUID currentUserId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AiResponseNotFoundException(postId));
        if (!post.isOwnedBy(currentUserId)) {
            throw new PostAccessDeniedException(postId);
        }
        AiResponse aiResponse = aiResponseRepository.findByPost_Id(postId)
                .orElseThrow(() -> new AiResponseNotFoundException(postId));
        return toDto(aiResponse);
    }

    private AiResponseDto toDto(AiResponse aiResponse) {
        return AiResponseDto.builder()
                .postId(aiResponse.getPost().getId())
                .status(aiResponse.getStatus())
                .content(aiResponse.getContent())
                .emoji(aiResponse.getEmoji())
                .errorMessage(aiResponse.getErrorMessage())
                .build();
    }
}
