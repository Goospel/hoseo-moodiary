package hoseo.moodiary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "토큰 갱신 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequestDto {

    @Schema(description = "로그인 시 발급받은 refresh token", example = "Xy7-aBcD...")
    @NotBlank(message = "refresh token 은 필수입니다.")
    private String refreshToken;
}
