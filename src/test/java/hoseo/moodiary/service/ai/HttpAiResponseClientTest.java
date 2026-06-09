package hoseo.moodiary.service.ai;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hoseo.moodiary.exception.AiInferenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link HttpAiResponseClient} 단위 테스트 — WireMock 으로 AI 추론 서버 mock.
 *
 * <p>Spring context 없이 어댑터 인스턴스를 직접 생성해서 HTTP 송수신/에러 매핑 로직만 격리 테스트.
 *
 * <p><b>커버 분기</b>:
 * <ul>
 *   <li>happy path — 200 {message, emoji} → AiInferenceResult (message → content 매핑) + 요청 본문 4필드 전송 검증</li>
 *   <li>AI 서버 4xx → AiInferenceException</li>
 *   <li>AI 서버 5xx → AiInferenceException</li>
 *   <li>타임아웃 → AiInferenceException</li>
 *   <li>필수 필드(message/emoji) 누락 → AiInferenceException</li>
 * </ul>
 */
class HttpAiResponseClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID POST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private HttpAiResponseClient client;

    @BeforeEach
    void setUp() {
        client = new HttpAiResponseClient(wireMock.baseUrl(), 2000);
    }

    @Nested
    @DisplayName("happy path — 정상 추론")
    class HappyPath {

        @Test
        @DisplayName("200 {message, emoji} → AiInferenceResult. 요청 본문에 userId/postId/title/content 전송")
        void success_returnsResultAndSendsRequestBody() {
            wireMock.stubFor(post(urlPathEqualTo("/inference"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    { "message": "오늘 하루도 수고했어요.", "emoji": "😊" }
                                    """)));

            AiInferenceResult result = client.invoke(USER_ID, POST_ID, "오늘의 기분", "친구를 만나서 즐거웠다");

            // message → content 매핑.
            assertThat(result.content()).isEqualTo("오늘 하루도 수고했어요.");
            assertThat(result.emoji()).isEqualTo("😊");

            // 요청 본문에 4개 필드가 모두 실려 나갔는지.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/inference"))
                    .withRequestBody(equalToJson("""
                            {
                              "userId": "00000000-0000-0000-0000-000000000001",
                              "postId": "11111111-1111-1111-1111-111111111111",
                              "title": "오늘의 기분",
                              "content": "친구를 만나서 즐거웠다"
                            }
                            """)));
        }

        @Test
        @DisplayName("응답에 추가 필드가 있어도 message/emoji 만 파싱 (unknown 무시)")
        void extraFields_ignored() {
            wireMock.stubFor(post(urlPathEqualTo("/inference"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    { "message": "응답", "emoji": "🥰", "score": 0.97, "model": "v2" }
                                    """)));

            AiInferenceResult result = client.invoke(USER_ID, POST_ID, "t", "c");

            assertThat(result.content()).isEqualTo("응답");
            assertThat(result.emoji()).isEqualTo("🥰");
        }
    }

    @Nested
    @DisplayName("실패 매핑 → AiInferenceException")
    class Failure {

        @Test
        @DisplayName("AI 서버 4xx → AiInferenceException (요청 거절)")
        void clientError_throws() {
            wireMock.stubFor(post(urlPathEqualTo("/inference"))
                    .willReturn(aResponse().withStatus(400).withBody("bad request")));

            assertThatThrownBy(() -> client.invoke(USER_ID, POST_ID, "t", "c"))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("요청 거절");
        }

        @Test
        @DisplayName("AI 서버 5xx → AiInferenceException (일시 장애)")
        void serverError_throws() {
            wireMock.stubFor(post(urlPathEqualTo("/inference"))
                    .willReturn(aResponse().withStatus(503).withBody("unavailable")));

            assertThatThrownBy(() -> client.invoke(USER_ID, POST_ID, "t", "c"))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("일시 장애");
        }

        @Test
        @DisplayName("응답 지연이 타임아웃 초과 → AiInferenceException (cause 보존)")
        void timeout_throws() {
            HttpAiResponseClient shortTimeout = new HttpAiResponseClient(wireMock.baseUrl(), 300);
            wireMock.stubFor(post(urlPathEqualTo("/inference"))
                    .willReturn(aResponse()
                            .withFixedDelay(1500)
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"message\": \"늦은 응답\", \"emoji\": \"😴\" }")));

            assertThatThrownBy(() -> shortTimeout.invoke(USER_ID, POST_ID, "t", "c"))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("호출 실패")
                    // read 타임아웃은 RestClientException(추출 실패) 또는 그 하위 ResourceAccessException 으로 온다.
                    .hasCauseInstanceOf(org.springframework.web.client.RestClientException.class);
        }

        @Test
        @DisplayName("message 누락 → AiInferenceException (파싱 실패)")
        void missingMessage_throws() {
            wireMock.stubFor(post(urlPathEqualTo("/inference"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"emoji\": \"😊\" }")));

            assertThatThrownBy(() -> client.invoke(USER_ID, POST_ID, "t", "c"))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("파싱 실패");
        }

        @Test
        @DisplayName("emoji 누락 → AiInferenceException (파싱 실패)")
        void missingEmoji_throws() {
            wireMock.stubFor(post(urlPathEqualTo("/inference"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"message\": \"응답은 있는데 이모지가 없음\" }")));

            assertThatThrownBy(() -> client.invoke(USER_ID, POST_ID, "t", "c"))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("파싱 실패");
        }
    }
}
