package hoseo.moodiary.service.oauth2;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hoseo.moodiary.exception.OAuth2VerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link HttpGoogleOAuth2Provider} 단위 테스트 — WireMock 으로 Google tokeninfo endpoint mock.
 *
 * <p>Spring context 없이 provider 인스턴스를 직접 생성해서 HTTP 검증 로직만 격리 테스트.
 *
 * <p><b>커버 분기</b>:
 * <ul>
 *   <li>happy path — 모든 필드 정상 → OAuth2UserInfo 추출</li>
 *   <li>name 누락 → email prefix 폴백</li>
 *   <li>Google 400 (무효 토큰) → OAuth2VerificationException</li>
 *   <li>aud 불일치 (다른 앱 토큰) → OAuth2VerificationException</li>
 *   <li>email_verified=false → OAuth2VerificationException</li>
 *   <li>Google 5xx → OAuth2VerificationException</li>
 *   <li>blank token → HTTP 호출 없이 즉시 reject</li>
 * </ul>
 */
class HttpGoogleOAuth2ProviderTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String EXPECTED_AUDIENCE = "my-client-id.apps.googleusercontent.com";
    private static final String OTHER_AUDIENCE = "other-app.apps.googleusercontent.com";

    private HttpGoogleOAuth2Provider provider;

    @BeforeEach
    void setUp() {
        provider = new HttpGoogleOAuth2Provider(
                wireMock.baseUrl() + "/tokeninfo",
                EXPECTED_AUDIENCE
        );
    }

    @Nested
    @DisplayName("happy path — 정상 토큰 검증")
    class HappyPath {

        @Test
        @DisplayName("aud 일치 + email_verified=true → OAuth2UserInfo 추출")
        void valid_returnsUserInfo() {
            wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                    .withQueryParam("id_token", equalTo("valid-token"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "aud": "%s",
                                      "sub": "google-12345",
                                      "email": "alice@example.com",
                                      "email_verified": "true",
                                      "name": "Alice"
                                    }
                                    """.formatted(EXPECTED_AUDIENCE))));

            OAuth2UserInfo info = provider.verifyAndExtract("valid-token");

            assertThat(info.providerId()).isEqualTo("google-12345");
            assertThat(info.email()).isEqualTo("alice@example.com");
            assertThat(info.nickname()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("name 누락 → email prefix 를 nickname 으로 폴백")
        void nameAbsent_fallsBackToEmailPrefix() {
            wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "aud": "%s",
                                      "sub": "google-12345",
                                      "email": "bob@example.com",
                                      "email_verified": "true"
                                    }
                                    """.formatted(EXPECTED_AUDIENCE))));

            OAuth2UserInfo info = provider.verifyAndExtract("token-without-name");

            assertThat(info.nickname()).isEqualTo("bob");
        }
    }

    @Nested
    @DisplayName("Provider 거절 / 응답 검증 실패")
    class Rejection {

        @Test
        @DisplayName("Google 400 (무효 토큰) → OAuth2VerificationException")
        void invalidToken_throws() {
            wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    { "error": "invalid_token", "error_description": "Invalid Value" }
                                    """)));

            assertThatThrownBy(() -> provider.verifyAndExtract("expired-or-fake"))
                    .isInstanceOf(OAuth2VerificationException.class)
                    .hasMessageContaining("거절");
        }

        @Test
        @DisplayName("aud 불일치 (다른 앱용 토큰) → OAuth2VerificationException (confused deputy 방어)")
        void audienceMismatch_throws() {
            wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "aud": "%s",
                                      "sub": "google-12345",
                                      "email": "eve@example.com",
                                      "email_verified": "true",
                                      "name": "Eve"
                                    }
                                    """.formatted(OTHER_AUDIENCE))));

            assertThatThrownBy(() -> provider.verifyAndExtract("other-app-token"))
                    .isInstanceOf(OAuth2VerificationException.class)
                    .hasMessageContaining("audience 불일치");
        }

        @Test
        @DisplayName("email_verified=false → OAuth2VerificationException")
        void emailNotVerified_throws() {
            wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {
                                      "aud": "%s",
                                      "sub": "google-12345",
                                      "email": "unverified@example.com",
                                      "email_verified": "false",
                                      "name": "Carol"
                                    }
                                    """.formatted(EXPECTED_AUDIENCE))));

            assertThatThrownBy(() -> provider.verifyAndExtract("unverified-email-token"))
                    .isInstanceOf(OAuth2VerificationException.class)
                    .hasMessageContaining("email_verified");
        }

        @Test
        @DisplayName("Google 5xx 일시 장애 → OAuth2VerificationException (FE 가 재시도 / 재로그인 유도)")
        void serverError_throws() {
            wireMock.stubFor(get(urlPathEqualTo("/tokeninfo"))
                    .willReturn(aResponse()
                            .withStatus(503)
                            .withBody("Service Unavailable")));

            assertThatThrownBy(() -> provider.verifyAndExtract("any-token"))
                    .isInstanceOf(OAuth2VerificationException.class)
                    .hasMessageContaining("일시 장애");
        }
    }

    @Nested
    @DisplayName("입력 검증 — HTTP 호출 전 reject")
    class InputValidation {

        @Test
        @DisplayName("null 토큰 → 즉시 reject (Google 호출 X)")
        void nullToken_rejectedBeforeHttp() {
            assertThatThrownBy(() -> provider.verifyAndExtract(null))
                    .isInstanceOf(OAuth2VerificationException.class)
                    .hasMessageContaining("id_token 빈 값");
        }

        @Test
        @DisplayName("blank 토큰 → 즉시 reject")
        void blankToken_rejectedBeforeHttp() {
            assertThatThrownBy(() -> provider.verifyAndExtract("   "))
                    .isInstanceOf(OAuth2VerificationException.class)
                    .hasMessageContaining("id_token 빈 값");
        }
    }
}
