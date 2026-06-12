package hoseo.moodiary.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * AI 추론 어댑터 — 실제 AI 서버 HTTP 호출 구현 (Hugging Face Space).
 *
 * <p>활성화 조건: {@code ai.client.mode=http}. 기본값 {@code stub} 일 땐 비활성.
 *
 * <p><b>요청</b> ({@code POST {ai.server.url}/chat}):
 * <pre>{ "user_text": "일기 본문", "recent_emotions": "", "diary_date": "6월 11일" }</pre>
 * <ul>
 *   <li>{@code user_text} — 일기 본문. 빈 문자열 금지 (호출 전 {@code @NotBlank} 로 보장).</li>
 *   <li>{@code recent_emotions} — 최근 감정 요약. <b>현 단계 미사용 → 항상 빈 문자열</b> (AI 담당자 합의).</li>
 *   <li>{@code diary_date} — 일기 날짜를 {@code "M월 d일"} 한국어 포맷으로 (예: {@code "6월 11일"}).</li>
 * </ul>
 *
 * <p><b>응답 (성공)</b>:
 * <pre>{ "emotion": "neutral", "aiText": "...", "homeComment": "...", "diaryDate": "6월 11일" }</pre>
 * {@code aiText} → DB {@code ai_response_content}, {@code emotion}/{@code homeComment} → 전용 컬럼.
 * {@code diaryDate} 는 에코백이라 무시.
 *
 * <p><b>실패 매핑</b> → 전부 {@link AiInferenceException} (호출자가 FAILED 로 전이):
 * <ul>
 *   <li>4xx — 요청 거절 (재시도해도 무의미)</li>
 *   <li>5xx — AI 서버 일시 장애</li>
 *   <li>타임아웃 / 네트워크 / 응답 추출 실패 ({@link RestClientException} — {@code ResourceAccessException} 포함) — cause 보존 + log.warn (T-033)</li>
 *   <li>빈 응답 / 필수 필드(aiText·emotion) 누락 — 파싱 실패</li>
 * </ul>
 *
 * <p><b>⚠️ 타임아웃</b>: HF 무료 Space 는 절전(cold start) 후 첫 호출이 느리고, 내부 SAIFEX/감정 모델
 * 다단계라 응답이 수~수십 초 걸린다. {@code ai.timeout-ms} 기본값을 넉넉히(30s) 두고 운영에서 {@code AI_TIMEOUT_MS}
 * 로 조정한다. 초과 시 해당 일기의 AI 응답만 FAILED (일기 자체는 영향 없음).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.client.mode", havingValue = "http")
public class HttpAiResponseClient implements AiResponseClient {

    private static final String CHAT_PATH = "/chat";
    /** {@code recent_emotions} 는 현 단계 미사용 — 항상 빈 문자열로 전송 (AI 담당자 합의). */
    private static final String RECENT_EMOTIONS = "";
    /** AI 서버가 받는 날짜 포맷 — 예: {@code "6월 11일"}. 숫자 + 한글 리터럴이라 로케일 무관. */
    private static final DateTimeFormatter DIARY_DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일");

    private final RestClient restClient;

    public HttpAiResponseClient(
            @Value("${ai.server.url}") String serverUrl,
            @Value("${ai.timeout-ms:30000}") long timeoutMs
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
    public AiInferenceResult invoke(UUID postId, String content, LocalDate diaryDate) {
        ChatResponse response;
        try {
            response = restClient.post()
                    .uri(CHAT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatRequest(content, RECENT_EMOTIONS, diaryDate.format(DIARY_DATE_FORMAT)))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new AiInferenceException(
                                "AI 서버 요청 거절 — HTTP " + res.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new AiInferenceException(
                                "AI 서버 일시 장애 — HTTP " + res.getStatusCode().value());
                    })
                    .body(ChatResponse.class);
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

        // 필수 필드는 aiText·emotion. homeComment 는 없으면 빈 문자열로 관용 처리.
        if (response == null || isBlank(response.aiText()) || isBlank(response.emotion())) {
            throw new AiInferenceException(
                    "AI 서버 응답 파싱 실패 — aiText/emotion 누락 (postId=" + postId + ")");
        }
        String homeComment = response.homeComment() != null ? response.homeComment() : "";
        return new AiInferenceResult(response.aiText(), response.emotion(), homeComment);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** AI 서버({@code POST /chat})로 보내는 요청 본문. JSON key 는 snake_case 라 {@link JsonProperty} 로 매핑. */
    private record ChatRequest(
            @JsonProperty("user_text") String userText,
            @JsonProperty("recent_emotions") String recentEmotions,
            @JsonProperty("diary_date") String diaryDate
    ) {
    }

    /**
     * AI 서버 응답 (성공). 추가 필드가 와도 깨지지 않게 unknown 무시.
     * {@code aiText} 는 우리 {@code ai_response_content} 로 매핑. {@code diaryDate} 는 에코백이라 사용 안 함.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResponse(String emotion, String aiText, String homeComment, String diaryDate) {
    }
}
