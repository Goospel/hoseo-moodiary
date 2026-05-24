package hoseo.moodiary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원가입 요청")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserSignupRequestDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "이메일 (로그인 ID로 사용)", example = "user@example.com")
    private String email;

    /**
     * 영어 + 숫자 조합 최소 8글자.
     * <p>{@code (?=.*[A-Za-z])} 알파벳 1자 이상, {@code (?=.*\d)} 숫자 1자 이상,
     * {@code [A-Za-z\d]{8,}} 영문자/숫자만 8자 이상 (특수문자 불가).
     */
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
            message = "비밀번호는 영문과 숫자를 포함한 8자 이상이어야 합니다."
    )
    @Schema(description = "비밀번호 (영문+숫자 8자 이상)", example = "password123")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
    @Schema(description = "닉네임", example = "무디아리")
    private String nickname;
}
