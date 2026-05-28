package hoseo.moodiary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoseo.moodiary.config.SecurityConfig;
import hoseo.moodiary.dto.request.LoginRequestDto;
import hoseo.moodiary.dto.request.LogoutRequestDto;
import hoseo.moodiary.dto.request.TokenRefreshRequestDto;
import hoseo.moodiary.dto.request.UserSignupRequestDto;
import hoseo.moodiary.dto.response.LoginResponseDto;
import hoseo.moodiary.dto.response.TokenRefreshResponseDto;
import hoseo.moodiary.exception.DuplicateEmailException;
import hoseo.moodiary.exception.DuplicateNicknameException;
import hoseo.moodiary.exception.InvalidCredentialsException;
import hoseo.moodiary.exception.InvalidRefreshTokenException;
import hoseo.moodiary.security.JwtTokenProvider;
import hoseo.moodiary.service.UserService;
import hoseo.moodiary.service.oauth2.OAuth2Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 웹 레이어 단위 테스트.
 *
 * <p>{@link SecurityConfig}를 명시적으로 임포트해야 한다 — 이 설정은 {@code @WebMvcTest}의 기본
 * 컴포넌트 스캔에 포함되지 않아 임포트하지 않으면 Spring Security 기본값(전부 인증 요구) 적용 → 회원가입 호출 자체가 401.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    /** PR 12-pre — AuthController 가 OAuth2Service 를 주입 받음. 다른 endpoint 테스트와 분리. */
    @MockitoBean
    private OAuth2Service oauth2Service;

    /** SecurityConfig 가 JwtAuthenticationFilter 빈을 생성할 때 의존성으로 요구. 컨트롤러 테스트에서는 사용 안 함. */
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static String validBody(ObjectMapper om, String email, String password, String nickname) throws Exception {
        return om.writeValueAsString(UserSignupRequestDto.builder()
                .email(email).password(password).nickname(nickname).build());
    }

    @Nested
    @DisplayName("POST /auth/signup — 회원가입")
    class Signup {

        @Test
        @DisplayName("정상 입력이면 201과 생성된 사용자 ID를 반환한다")
        void success() throws Exception {
            UUID newId = UUID.randomUUID();
            given(userService.signup(any(UserSignupRequestDto.class))).willReturn(newId);

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody(objectMapper, "a@b.com", "password123", "nick")))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("\"" + newId + "\""));
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400과 검증 메시지를 반환한다")
        void invalidEmail() throws Exception {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody(objectMapper, "not-an-email", "password123", "nick")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."));

            verify(userService, never()).signup(any());
        }

        @Test
        @DisplayName("비밀번호가 영문만으로 8자여도 400 (숫자 없음)")
        void passwordMissingDigit() throws Exception {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody(objectMapper, "a@b.com", "onlyletters", "nick")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("비밀번호는 영문과 숫자를 포함한 8자 이상이어야 합니다."));
        }

        @Test
        @DisplayName("비밀번호가 7자면 400 (길이 미달)")
        void passwordTooShort() throws Exception {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody(objectMapper, "a@b.com", "abc1234", "nick")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("비밀번호는 영문과 숫자를 포함한 8자 이상이어야 합니다."));
        }

        @Test
        @DisplayName("닉네임이 1자면 400 (길이 미달)")
        void nicknameTooShort() throws Exception {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody(objectMapper, "a@b.com", "password123", "n")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("닉네임은 2자 이상 20자 이하여야 합니다."));
        }

        @Test
        @DisplayName("이메일 중복이면 409와 도메인 메시지를 반환한다")
        void duplicateEmail() throws Exception {
            willThrow(new DuplicateEmailException("a@b.com"))
                    .given(userService).signup(any(UserSignupRequestDto.class));

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody(objectMapper, "a@b.com", "password123", "nick")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다. email=a@b.com"));
        }

        @Test
        @DisplayName("닉네임 중복이면 409와 도메인 메시지를 반환한다")
        void duplicateNickname() throws Exception {
            willThrow(new DuplicateNicknameException("nick"))
                    .given(userService).signup(any(UserSignupRequestDto.class));

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody(objectMapper, "a@b.com", "password123", "nick")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다. nickname=nick"));
        }
    }

    @Nested
    @DisplayName("POST /auth/login — 로그인")
    class Login {

        private String loginBody(String email, String password) throws Exception {
            return objectMapper.writeValueAsString(LoginRequestDto.builder()
                    .email(email).password(password).build());
        }

        @Test
        @DisplayName("이메일/비밀번호 정상이면 200과 accessToken + refreshToken + userId 셋 다 반환한다")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            given(userService.login(any(LoginRequestDto.class))).willReturn(LoginResponseDto.builder()
                    .accessToken("issued.jwt.token")
                    .refreshToken("issued-refresh-token")
                    .userId(userId)
                    .build());

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("a@b.com", "password123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("issued.jwt.token"))
                    .andExpect(jsonPath("$.refreshToken").value("issued-refresh-token"))
                    .andExpect(jsonPath("$.userId").value(userId.toString()));
        }

        @Test
        @DisplayName("이메일이 비어있으면 400과 검증 메시지를 반환한다")
        void blankEmail() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이메일은 필수입니다."));

            verify(userService, never()).login(any());
        }

        @Test
        @DisplayName("비밀번호가 비어있으면 400과 검증 메시지를 반환한다")
        void blankPassword() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("a@b.com", "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("비밀번호는 필수입니다."));
        }

        @Test
        @DisplayName("자격 증명 불일치면 401과 통일 메시지를 반환한다 (이메일/비번 구분 안 함)")
        void invalidCredentials() throws Exception {
            willThrow(new InvalidCredentialsException())
                    .given(userService).login(any(LoginRequestDto.class));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("a@b.com", "wrong")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh — 토큰 갱신")
    class Refresh {

        private String refreshBody(String token) throws Exception {
            return objectMapper.writeValueAsString(TokenRefreshRequestDto.builder().refreshToken(token).build());
        }

        @Test
        @DisplayName("정상 refresh 면 200과 새 access + 새 refresh 반환")
        void success() throws Exception {
            given(userService.refresh("old-refresh")).willReturn(TokenRefreshResponseDto.builder()
                    .accessToken("new.access.jwt")
                    .refreshToken("new-refresh")
                    .build());

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshBody("old-refresh")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new.access.jwt"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
        }

        @Test
        @DisplayName("refresh token 이 빈 값이면 400 + service 미호출")
        void blank() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshBody("")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("refresh token 은 필수입니다."));

            verify(userService, never()).refresh(any());
        }

        @Test
        @DisplayName("유효하지 않은 refresh 면 401과 통일 메시지")
        void invalid() throws Exception {
            willThrow(new InvalidRefreshTokenException())
                    .given(userService).refresh("invalid");

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshBody("invalid")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("유효하지 않은 refresh token 입니다."));
        }
    }

    @Nested
    @DisplayName("POST /auth/logout — 로그아웃")
    class Logout {

        private String logoutBody(String token) throws Exception {
            return objectMapper.writeValueAsString(LogoutRequestDto.builder().refreshToken(token).build());
        }

        @Test
        @DisplayName("정상 logout 이면 204 + service.logout 호출")
        void success() throws Exception {
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(logoutBody("some-refresh")))
                    .andExpect(status().isNoContent());

            verify(userService).logout(eq("some-refresh"));
        }

        @Test
        @DisplayName("refresh token 이 빈 값이면 400 + service 미호출")
        void blank() throws Exception {
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(logoutBody("")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("refresh token 은 필수입니다."));

            verify(userService, never()).logout(any());
        }
    }
}
