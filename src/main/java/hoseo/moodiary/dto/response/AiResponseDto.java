package hoseo.moodiary.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import hoseo.moodiary.entitiy.AiResponseStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * AI 응답 폴링 응답 (GET /post/{id}/ai-response).
 *
 * <p>{@code content} / {@code emotion} / {@code homeComment} 는 상태에 따라 null 일 수 있다
 * (PENDING / FAILED) — JSON 에 그대로 노출. {@code errorMessage} 만 FAILED 일 때 채워지고
 * PENDING/DONE 에선 null → {@code @JsonInclude(NON_NULL)} 로 응답에서 생략.
 *
 * <p>이 비대칭은 api-contracts 의 예시 JSON 모양에 맞춤 — PENDING 응답은 {@code content/emotion/homeComment}
 * 가 null 까지 명시되지만 {@code errorMessage} 는 키 자체가 없음.
 */
@Getter
@Builder
public class AiResponseDto {

    private UUID postId;
    private AiResponseStatus status;
    private String content;
    private String emotion;
    private String homeComment;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;
}
