package hoseo.moodiary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "로그인 응답")
@Getter
@Builder
public class LoginResponseDto {

    @Schema(description = "JWT access token. 이후 모든 보호된 요청에 'Authorization: Bearer <accessToken>' 헤더로 첨부",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "로그인된 사용자의 ID")
    private UUID userId;
}
