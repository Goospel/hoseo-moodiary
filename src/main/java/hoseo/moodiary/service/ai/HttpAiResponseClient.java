package hoseo.moodiary.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hoseo.moodiary.exception.AiInferenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.UUID;

/**
 * AI 추론 어댑터 — 실제 AI 서버 HTTP 호출 구현.
 *
 * <p>활성화 조건: {@code ai.client.mode=http}. 기본값 {@code stub} 일 땐 비활성.
 *
 * <p><b>⚠️ 잠정 계약 (provisional)</b>: AI 서버가 아직 미배포 + 응답 형태 미확정인 상태에서
 * <i>우리 측 제안</i>으로 먼저 구현해 둔 것. OAuth2 어댑터와 같이 토글 뒤에 두어 운영(stub)은 안 깨지게 하고,
 * AI 서버가 실체화되면 URL / 필드명 / 인증을 맞춘다. 인증은 현 단계에서 보류(헤더 없음).
 *
 * <p><b>요청</b> ({@code POST {ai.server.url}/inference}):
 * <pre>{ "userId": "...", "postId": "...", "title": "...", "content": "..." }</pre>
 *
 * <p><b>응답 (성공)</b>:
 * <pre>{ "message": "AI 도출 메시지", "emoji": "😊" }</pre>
 * {@code message} 는 우리 DB 의 {@code ai_response_content} 로 매핑된다.
 *
 * <p><b>실패 매핑</b> → 전부 {@link AiInferenceException} (호출자가 FAILED 로 전이):
 * <ul>
 *   <li>4xx — 요청 거절 (재시도해도 무의미)</li>
 *   <li>5xx — AI 서버 일시 장애</li>
 *   <li>타임아웃 / 네트워크 / 응답 추출 실패 ({@link RestClientException} — {@code ResourceAccessException} 포함) — cause 보존 + log.warn (T-033)</li>
 *   <li>빈 응답 / 필수 필드(message·emoji) 누락 — 파싱 실패</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.client.mode", havingValue = "http")
public class HttpAiResponseClient implements AiResponseClient {

    private static final String INFERENCE_PATH = "/inference";

    private final RestClient restClient;

    public HttpAiResponseClient(
            @Value("${ai.server.url}") String serverUrl,
            @Value("${ai.timeout-ms:10000}") long timeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public AiInferenceResult invoke(UUID userId, UUID postId, String title, String content) {
        InferenceResponse response;
        try {
            response = restClient.post()
                    .uri(INFERENCE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new InferenceRequest(userId, postId, title, content))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new AiInferenceException(
                                "AI 서버 요청 거절 — HTTP " + res.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new AiInferenceException(
                                "AI 서버 일시 장애 — HTTP " + res.getStatusCode().value());
                    })
                    .body(InferenceResponse.class);
        } catch (RestClientException e) {
            // 타임아웃(connect/read) / DNS / connection refused / 응답 추출 실패 등 transport 계열.
            // ResourceAccessException 은 RestClientException 의 하위라 함께 잡힌다. read 타임아웃이
            // body 추출 도중 터지면 ResourceAccessException 이 아니라 RestClientException 으로 오기 때문에
            // 넓게 잡아야 누락이 없다. onStatus 의 AiInferenceException 은 RestClientException 이 아니므로
            // 여기서 안 잡히고 그대로 전파된다.
            // T-033: cause 보존 + log.warn 으로 운영 진단 가능하게.
            log.warn("AI 서버 호출 실패 — 타임아웃/네트워크/응답 추출 문제 (postId={})", postId, e);
            throw new AiInferenceException("AI 서버 호출 실패: " + e.getMessage(), e);
        }

        if (response == null || isBlank(response.message()) || isBlank(response.emoji())) {
            throw new AiInferenceException(
                    "AI 서버 응답 파싱 실패 — message/emoji 누락 (postId=" + postId + ")");
        }
        return new AiInferenceResult(response.message(), response.emoji());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** AI 서버로 보내는 요청 본문 (잠정 계약). */
    private record InferenceRequest(UUID userId, UUID postId, String title, String content) {
    }

    /**
     * AI 서버 응답 (성공). 추가 필드가 와도 깨지지 않게 unknown 무시.
     * {@code message} 는 우리 {@code ai_response_content} 로 매핑.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InferenceResponse(String message, String emoji) {
    }
}
