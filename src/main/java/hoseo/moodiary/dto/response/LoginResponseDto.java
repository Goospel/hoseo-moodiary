package hoseo.moodiary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "로그인 응답")
@Getter
@Builder
public class LoginResponseDto {

    @Schema(description = "JWT access token (1시간 수명). 이후 보호된 요청에 'Authorization: Bearer <accessToken>' 헤더로 첨부",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Refresh token (2주 수명). access token 만료 시 POST /auth/refresh 로 갱신할 때 사용. " +
            "탈취 시 노출 창이 크므로 안전하게 보관 — localStorage 사용 시 XSS 주의.",
            example = "Xy7-aBcD...")
    private String refreshToken;

    @Schema(description = "로그인된 사용자의 ID")
    private UUID userId;
}
