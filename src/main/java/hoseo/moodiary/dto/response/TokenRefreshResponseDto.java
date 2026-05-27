package hoseo.moodiary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "토큰 갱신 응답 — 새 access + 새 refresh (rotation 정책)")
@Getter
@Builder
public class TokenRefreshResponseDto {

    @Schema(description = "새로 발급된 JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "새로 발급된 refresh token — 기존 refresh 는 무효화됨 (rotation)",
            example = "Xy7-aBcD...")
    private String refreshToken;
}
