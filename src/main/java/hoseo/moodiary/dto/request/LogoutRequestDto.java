package hoseo.moodiary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로그아웃 요청 — refresh token 을 무효화한다. access token 은 stateless 라 서버에서 즉시 차단 불가, 만료 대기.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequestDto {

    @Schema(description = "무효화할 refresh token")
    @NotBlank(message = "refresh token 은 필수입니다.")
    private String refreshToken;
}
