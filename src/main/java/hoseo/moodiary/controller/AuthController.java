package hoseo.moodiary.controller;

import hoseo.moodiary.dto.request.LoginRequestDto;
import hoseo.moodiary.dto.request.LogoutRequestDto;
import hoseo.moodiary.dto.request.TokenRefreshRequestDto;
import hoseo.moodiary.dto.request.UserSignupRequestDto;
import hoseo.moodiary.dto.response.LoginResponseDto;
import hoseo.moodiary.dto.response.TokenRefreshResponseDto;
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

@Tag(name = "Auth", description = "인증 API (회원가입 / 로그인 / 토큰 갱신 / 로그아웃)")
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
            description = "이메일/비밀번호로 로그인. 성공 시 access token (1h) + refresh token (2w) 같이 발급. " +
                    "이후 보호된 엔드포인트는 'Authorization: Bearer <accessToken>' 헤더로 호출. " +
                    "access 만료 시 POST /auth/refresh 로 갱신.")
    @ApiResponse(responseCode = "200", description = "로그인 성공 — access + refresh token + userId 반환")
    @ApiResponse(responseCode = "400", description = "이메일/비밀번호 빈 값")
    @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        return ResponseEntity.ok(userService.login(requestDto));
    }

    @Operation(summary = "토큰 갱신 (refresh)",
            description = "Refresh token 으로 새 access + 새 refresh 발급 (rotation). " +
                    "기존 refresh 는 즉시 무효화됨 — 동일 refresh 로 두 번 호출 시 두 번째는 401.")
    @ApiResponse(responseCode = "200", description = "갱신 성공 — 새 access + 새 refresh 반환")
    @ApiResponse(responseCode = "400", description = "refresh token 빈 값")
    @ApiResponse(responseCode = "401", description = "refresh token 이 유효하지 않음 (존재 X / 만료 / 이미 revoke)")
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponseDto> refresh(@Valid @RequestBody TokenRefreshRequestDto requestDto) {
        return ResponseEntity.ok(userService.refresh(requestDto.getRefreshToken()));
    }

    @Operation(summary = "로그아웃",
            description = "Refresh token 을 무효화한다. Access token 은 stateless 라 서버에서 즉시 차단 불가 — " +
                    "FE 는 logout 호출과 함께 localStorage 의 토큰도 즉시 제거해야 함. " +
                    "이미 무효화된 토큰을 다시 보내도 204 (idempotent).")
    @ApiResponse(responseCode = "204", description = "로그아웃 성공 (응답 body 없음)")
    @ApiResponse(responseCode = "400", description = "refresh token 빈 값")
    @PostMapping("/logout")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequestDto requestDto) {
        userService.logout(requestDto.getRefreshToken());
    }
}
