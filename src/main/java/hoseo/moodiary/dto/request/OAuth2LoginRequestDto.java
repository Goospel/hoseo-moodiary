package hoseo.moodiary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth2 로그인 / 가입 요청 — FE 가 Google Sign-In 으로 발급받은 token 을 BE 로 넘긴다.
 *
 * <p>BE 는 이 토큰을 Google {@code tokeninfo} endpoint 에 검증 + 사용자 정보 추출 →
 * 우리 DB 의 {@code (provider, providerId)} 로 기존 사용자 조회 → 있으면 로그인 / 없으면 신규 가입 + 로그인.
 *
 * <p>Stub 모드 ({@code oauth2.client.mode=stub}) 에서는 {@code providerAccessToken} 이
 * {@code stub:{provider}:{providerId}:{email}:{nickname}} 형식 — 외부 Console 셋업 / HTTP 없이 단독 테스트 가능.
 *
 * <p>Kakao 는 졸업프로젝트 범위에서 보류 (AuthProvider Javadoc 참조).
 */
@Schema(description = "OAuth2 로그인 / 가입 요청 — FE 가 Google 측 token 을 BE 로 넘김")
@Getter
@NoArgsConstructor
public class OAuth2LoginRequestDto {

    @Schema(
            description = "Google Sign-In 이 FE 에게 발급한 id_token (또는 access_token). Stub 모드에서는 " +
                    "`stub:{PROVIDER}:{providerId}:{email}:{nickname}` 형식 (예: `stub:GOOGLE:google-123:alice@example.com:Alice`).",
            example = "stub:GOOGLE:google-123:alice@example.com:Alice"
    )
    @NotBlank(message = "providerAccessToken 은 빈 값일 수 없습니다.")
    private String providerAccessToken;

    public OAuth2LoginRequestDto(String providerAccessToken) {
        this.providerAccessToken = providerAccessToken;
    }
}
