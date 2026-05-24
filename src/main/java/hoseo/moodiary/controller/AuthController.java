package hoseo.moodiary.controller;

import hoseo.moodiary.dto.request.LoginRequestDto;
import hoseo.moodiary.dto.request.UserSignupRequestDto;
import hoseo.moodiary.dto.response.LoginResponseDto;
import hoseo.moodiary.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Auth", description = "인증 API (회원가입 / 로그인)")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임으로 새 계정을 만든다. 비밀번호는 BCrypt로 해시되어 저장.")
    @ApiResponse(responseCode = "201", description = "회원가입 성공 — 생성된 사용자 ID 반환")
    @ApiResponse(responseCode = "400", description = "입력값 검증 실패 (이메일 형식/비밀번호 규칙/닉네임 길이 등)")
    @ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복")
    @PostMapping("/signup")
    public ResponseEntity<UUID> signup(@Valid @RequestBody UserSignupRequestDto requestDto) {
        UUID userId = userService.signup(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    @Operation(summary = "로그인",
            description = "이메일/비밀번호로 로그인. 성공 시 JWT access token 발급. " +
                    "이후 보호된 엔드포인트는 'Authorization: Bearer <accessToken>' 헤더로 호출.")
    @ApiResponse(responseCode = "200", description = "로그인 성공 — access token + userId 반환")
    @ApiResponse(responseCode = "400", description = "이메일/비밀번호 빈 값")
    @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        return ResponseEntity.ok(userService.login(requestDto));
    }
}
