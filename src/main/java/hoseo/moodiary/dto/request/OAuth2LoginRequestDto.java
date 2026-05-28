package hoseo.moodiary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth2 로그인 / 가입 요청 — FE 가 provider (Google / Kakao) 에서 발급받은 access token 을 BE 로 넘긴다.
 *
 * <p>BE 는 이 토큰을 provider 서버 (Google {@code tokeninfo} / Kakao {@code /v2/user/me}) 에 검증 + 사용자 정보 추출 →
 * 우리 DB 의 {@code (provider, providerId)} 로 기존 사용자 조회 → 있으면 로그인 / 없으면 신규 가입 + 로그인.
 *
 * <p>PR 12-pre 단계의 Stub 모드에서는 {@code providerAccessToken} 이 {@code stub:{provider}:{providerId}:{email}:{nickname}}
 * 형식 — 외부 Console 셋업 / HTTP 없이 단독 테스트 가능.
 */
@Schema(description = "OAuth2 로그인 / 가입 요청 — FE 가 provider 측 access token 을 BE 로 넘김")
@Getter
@NoArgsConstructor
public class OAuth2LoginRequestDto {

    @Schema(
            description = "Provider (Google / Kakao) 가 FE 에게 발급한 access token. PR 12-pre Stub 모드에서는 " +
                    "`stub:{PROVIDER}:{providerId}:{email}:{nickname}` 형식 (예: `stub:GOOGLE:google-123:alice@example.com:Alice`).",
            example = "stub:GOOGLE:google-123:alice@example.com:Alice"
    )
    @NotBlank(message = "providerAccessToken 은 빈 값일 수 없습니다.")
    private String providerAccessToken;

    public OAuth2LoginRequestDto(String providerAccessToken) {
        this.providerAccessToken = providerAccessToken;
    }
}
