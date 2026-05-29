package hoseo.moodiary.service.oauth2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import hoseo.moodiary.entitiy.AuthProvider;
import hoseo.moodiary.exception.OAuth2VerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Google OAuth2 provider — 실제 Google tokeninfo endpoint 호출 구현.
 *
 * <p>활성화 조건: {@code oauth2.client.mode=http} (운영 / 시연). 기본값 {@code stub} 일 땐 비활성.
 *
 * <p><b>흐름</b>:
 * <ol>
 *   <li>FE 가 Google Sign-In (Google Identity Services) 으로 받은 {@code id_token} 을 BE 로 전달.</li>
 *   <li>BE 가 {@code GET https://oauth2.googleapis.com/tokeninfo?id_token=<token>} 호출.</li>
 *   <li>Google 이 토큰의 서명 / 만료 / iss 등 표준 검증을 수행 — 200 OK 면 통과, 400 이면 무효.</li>
 *   <li>BE 가 응답을 추가 검증:
 *     <ul>
 *       <li>{@code aud} = 우리 {@code GOOGLE_OAUTH_CLIENT_ID} — 다른 앱용 토큰으로 우리 서비스 가입 시도 (confused deputy) 차단.</li>
 *       <li>{@code email_verified == "true"} — Google 측 미검증 이메일 차단.</li>
 *     </ul>
 *   </li>
 *   <li>{@code sub} / {@code email} / {@code name} 을 {@link OAuth2UserInfo} 로 추출.</li>
 * </ol>
 *
 * <p><b>토큰 형식</b>: id_token (JWT) 권장. access_token 도 같은 endpoint 가 받지만 응답에 {@code name} 이 빠져서
 * 닉네임 자리에 email prefix 가 들어감 — FE 가 id_token 을 보내도록 컨벤션 합의 권장.
 *
 * <p><b>비밀 키 정책</b>: client_secret 은 사용하지 않는다 — tokeninfo 검증은 client_id 만 필요 (audience 비교용).
 * 운영에선 {@code GOOGLE_OAUTH_CLIENT_ID} env var 만 주입.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "oauth2.client.mode", havingValue = "http")
public class HttpGoogleOAuth2Provider implements OAuth2Provider {

    private static final String EMAIL_VERIFIED_TRUE = "true";

    private final RestClient restClient;
    private final String tokeninfoUrl;
    private final String expectedAudience;

    public HttpGoogleOAuth2Provider(
            @Value("${oauth2.google.tokeninfo-url}") String tokeninfoUrl,
            @Value("${oauth2.google.client-id}") String expectedAudience
    ) {
        this.tokeninfoUrl = tokeninfoUrl;
        this.expectedAudience = expectedAudience;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuth2UserInfo verifyAndExtract(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new OAuth2VerificationException("id_token 빈 값");
        }

        GoogleTokenInfo info;
        try {
            info = restClient.get()
                    .uri(tokeninfoUrl + "?id_token={token}", idToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        // Google 은 무효 토큰에 400 + {error, error_description} 본문을 돌려준다.
                        throw new OAuth2VerificationException(
                                "Google tokeninfo 토큰 거절 — HTTP " + res.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        // 5xx 는 우리 책임 아님 — 401 로 변환해서 FE 가 재시도 / 재로그인 유도.
                        throw new OAuth2VerificationException(
                                "Google tokeninfo 일시 장애 — HTTP " + res.getStatusCode().value());
                    })
                    .body(GoogleTokenInfo.class);
        } catch (ResourceAccessException e) {
            // 네트워크 / DNS / connection refused 등 — provider 측 검증 실패로 통일.
            // T-033: 운영에서 Google 외부 통신 장애 추적 가능하도록 cause chain 보존 + log.warn.
            log.warn("Google tokeninfo 호출 실패 — 네트워크/DNS/connection 문제로 OAuth2 가입 차단", e);
            throw new OAuth2VerificationException("Google tokeninfo 호출 실패: " + e.getMessage(), e);
        }

        if (info == null) {
            throw new OAuth2VerificationException("Google tokeninfo 빈 응답");
        }

        // audience (token confused deputy 방어) — 우리 client_id 가 발급 대상이 아니면 거절.
        if (info.aud() == null || !info.aud().equals(expectedAudience)) {
            throw new OAuth2VerificationException(
                    "Google tokeninfo audience 불일치 — 우리 client_id 용 토큰이 아님");
        }

        // 이메일이 Google 측에서 검증된 계정만 허용 — 미검증 이메일로 가짜 가입 차단.
        if (!EMAIL_VERIFIED_TRUE.equals(info.emailVerified())) {
            throw new OAuth2VerificationException("Google tokeninfo email_verified=false");
        }

        if (info.sub() == null || info.sub().isBlank()) {
            throw new OAuth2VerificationException("Google tokeninfo sub 빈 값");
        }
        if (info.email() == null || info.email().isBlank()) {
            throw new OAuth2VerificationException("Google tokeninfo email 빈 값");
        }

        // name 이 비면 email prefix 로 폴백 — 닉네임 충돌 처리는 OAuth2Service 에서.
        String nickname = (info.name() != null && !info.name().isBlank())
                ? info.name()
                : info.email().split("@")[0];

        return new OAuth2UserInfo(info.sub(), info.email(), nickname);
    }

    /**
     * Google tokeninfo 응답 — 우리가 사용하는 필드만.
     *
     * <p>Google 응답에는 {@code iss / azp / iat / exp / hd / picture} 등 더 많은 필드가 있지만 무시한다.
     * Jackson 의 {@code @JsonIgnoreProperties(ignoreUnknown=true)} 로 추가 필드 등장 시에도 깨지지 않게.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenInfo(
            String aud,
            String sub,
            String email,
            @JsonProperty("email_verified") String emailVerified,
            String name
    ) {
    }
}
