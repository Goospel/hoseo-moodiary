package hoseo.moodiary.service.ai;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hoseo.moodiary.exception.AiInferenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
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
 * {@link HttpAiResponseClient} 단위 테스트 — WireMock 으로 AI 추론 서버(Hugging Face Space) mock.
 *
 * <p>Spring context 없이 어댑터 인스턴스를 직접 생성해서 HTTP 송수신/에러 매핑 로직만 격리 테스트.
 *
 * <p><b>커버 분기</b>:
 * <ul>
 *   <li>happy path — 200 {emotion, aiText, homeComment} → AiInferenceResult 매핑 + 요청 본문(user_text/recent_emotions/diary_date) 검증</li>
 *   <li>diary_date 한국어 포맷 ("M월 d일") 변환 검증</li>
 *   <li>homeComment 누락 → 빈 문자열로 관용 (실패 아님)</li>
 *   <li>AI 서버 4xx / 5xx / 타임아웃 → AiInferenceException</li>
 *   <li>필수 필드(aiText/emotion) 누락 → AiInferenceException</li>
 * </ul>
 */
class HttpAiResponseClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final UUID POST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate DIARY_DATE = LocalDate.of(2026, 6, 11);

    private HttpAiResponseClient client;

    @BeforeEach
    void setUp() {
        client = new HttpAiResponseClient(wireMock.baseUrl(), 2000);
    }

    @Nested
    @DisplayName("happy path — 정상 추론")
    class HappyPath {

        @Test
        @DisplayName("200 {emotion, aiText, homeComment} → AiInferenceResult. 요청 본문에 user_text/recent_emotions/diary_date 전송")
        void success_returnsResultAndSendsRequestBody() {
            wireMock.stubFor(post(urlPathEqualTo("/chat"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "emotion": "happy",
                                      "aiText": "오늘 하루도 수고했어요.",
                                      "homeComment": "좋은 하루였네요!",
                                      "diaryDate": "6월 11일"
                                    }
                                    """)));

            AiInferenceResult result = client.invoke(POST_ID, "친구를 만나서 즐거웠다", DIARY_DATE);

            // aiText → content 매핑, emotion/homeComment 그대로.
            assertThat(result.content()).isEqualTo("오늘 하루도 수고했어요.");
            assertThat(result.emotion()).isEqualTo("happy");
            assertThat(result.homeComment()).isEqualTo("좋은 하루였네요!");

            // 요청 본문: user_text=일기, recent_emotions="" 고정, diary_date="6월 11일" 한국어 포맷.
            wireMock.verify(postRequestedFor(urlPathEqualTo("/chat"))
                    .withRequestBody(equalToJson("""
                            {
                              "user_text": "친구를 만나서 즐거웠다",
                              "recent_emotions": "",
                              "diary_date": "6월 11일"
                            }
                            """)));
        }

        @Test
        @DisplayName("응답에 추가 필드가 있어도 필요한 필드만 파싱 (unknown 무시)")
        void extraFields_ignored() {
            wireMock.stubFor(post(urlPathEqualTo("/chat"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    { "emotion": "neutral", "aiText": "응답", "homeComment": "홈", "score": 0.97, "model": "v2" }
                                    """)));

            AiInferenceResult result = client.invoke(POST_ID, "c", DIARY_DATE);

            assertThat(result.content()).isEqualTo("응답");
            assertThat(result.emotion()).isEqualTo("neutral");
            assertThat(result.homeComment()).isEqualTo("홈");
        }

        @Test
        @DisplayName("homeComment 누락 → 빈 문자열로 관용 (실패 아님)")
        void missingHomeComment_defaultsToEmpty() {
            wireMock.stubFor(post(urlPathEqualTo("/chat"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    { "emotion": "neutral", "aiText": "본문은 있음" }
                                    """)));

            AiInferenceResult result = client.invoke(POST_ID, "c", DIARY_DATE);

            assertThat(result.content()).isEqualTo("본문은 있음");
            assertThat(result.emotion()).isEqualTo("neutral");
            assertThat(result.homeComment()).isEmpty();
        }
    }

    @Nested
    @DisplayName("실패 매핑 → AiInferenceException")
    class Failure {

        @Test
        @DisplayName("AI 서버 4xx → AiInferenceException (요청 거절)")
        void clientError_throws() {
            wireMock.stubFor(post(urlPathEqualTo("/chat"))
                    .willReturn(aResponse().withStatus(400).withBody("bad request")));

            assertThatThrownBy(() -> client.invoke(POST_ID, "c", DIARY_DATE))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("요청 거절");
        }

        @Test
        @DisplayName("AI 서버 5xx → AiInferenceException (일시 장애)")
        void serverError_throws() {
            wireMock.stubFor(post(urlPathEqualTo("/chat"))
                    .willReturn(aResponse().withStatus(503).withBody("unavailable")));

            assertThatThrownBy(() -> client.invoke(POST_ID, "c", DIARY_DATE))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("일시 장애");
        }

        @Test
        @DisplayName("응답 지연이 타임아웃 초과 → AiInferenceException (cause 보존)")
        void timeout_throws() {
            HttpAiResponseClient shortTimeout = new HttpAiResponseClient(wireMock.baseUrl(), 300);
            wireMock.stubFor(post(urlPathEqualTo("/chat"))
                    .willReturn(aResponse()
                            .withFixedDelay(1500)
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"emotion\": \"tired\", \"aiText\": \"늦은 응답\", \"homeComment\": \"\" }")));

            assertThatThrownBy(() -> shortTimeout.invoke(POST_ID, "c", DIARY_DATE))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("호출 실패")
                    // read 타임아웃은 RestClientException(추출 실패) 또는 그 하위 ResourceAccessException 으로 온다.
                    .hasCauseInstanceOf(org.springframework.web.client.RestClientException.class);
        }

        @Test
        @DisplayName("aiText 누락 → AiInferenceException (파싱 실패)")
        void missingAiText_throws() {
            wireMock.stubFor(post(urlPathEqualTo("/chat"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"emotion\": \"neutral\", \"homeComment\": \"홈\" }")));

            assertThatThrownBy(() -> client.invoke(POST_ID, "c", DIARY_DATE))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("파싱 실패");
        }

        @Test
        @DisplayName("emotion 누락 → AiInferenceException (파싱 실패)")
        void missingEmotion_throws() {
            wireMock.stubFor(post(urlPathEqualTo("/chat"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"aiText\": \"본문은 있는데 감정이 없음\", \"homeComment\": \"홈\" }")));

            assertThatThrownBy(() -> client.invoke(POST_ID, "c", DIARY_DATE))
                    .isInstanceOf(AiInferenceException.class)
                    .hasMessageContaining("파싱 실패");
        }
    }
}
