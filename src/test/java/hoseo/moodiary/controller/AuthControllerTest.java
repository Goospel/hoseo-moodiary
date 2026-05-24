package hoseo.moodiary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoseo.moodiary.config.SecurityConfig;
import hoseo.moodiary.dto.request.UserSignupRequestDto;
import hoseo.moodiary.exception.DuplicateEmailException;
import hoseo.moodiary.exception.DuplicateNicknameException;
import hoseo.moodiary.service.UserService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 웹 레이어 단위 테스트 ({@code POST /auth/signup}).
 *
 * <p>{@link SecurityConfig}를 명시적으로 임포트해야 한다.
 * 이 설정은 {@code @WebMvcTest}의 기본 컴포넌트 스캔에 포함되지 않기 때문에,
 * 임포트하지 않으면 Spring Security 기본값(전부 인증 요구)이 적용되어 회원가입 호출 자체가 401이 된다.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

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
}
