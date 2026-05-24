package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.LoginRequestDto;
import hoseo.moodiary.dto.request.UserSignupRequestDto;
import hoseo.moodiary.dto.response.LoginResponseDto;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.DuplicateEmailException;
import hoseo.moodiary.exception.DuplicateNicknameException;
import hoseo.moodiary.exception.InvalidCredentialsException;
import hoseo.moodiary.repository.UserJpaRepository;
import hoseo.moodiary.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserService 단위 테스트 — 회원가입 분기, 비밀번호 해싱 위임, 로그인 분기 + JWT 발급 위임.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserJpaRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private static User userWithId(UUID id, String email, String hashed, String nickname) {
        User user = User.builder().email(email).password(hashed).nickname(nickname).build();
        try {
            Field f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return user;
    }

    @Nested
    @DisplayName("signup — 회원가입")
    class Signup {

        @Test
        @DisplayName("정상 입력이면 비밀번호를 해시한 채로 저장하고 ID를 반환한다")
        void success() {
            UUID newId = UUID.randomUUID();
            given(repository.existsByEmail("a@b.com")).willReturn(false);
            given(repository.existsByNickname("nick")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("HASHED");
            given(repository.save(any(User.class)))
                    .willReturn(userWithId(newId, "a@b.com", "HASHED", "nick"));

            UUID result = userService.signup(UserSignupRequestDto.builder()
                    .email("a@b.com")
                    .password("password123")
                    .nickname("nick")
                    .build());

            assertThat(result).isEqualTo(newId);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(repository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("a@b.com");
            assertThat(saved.getNickname()).isEqualTo("nick");
            // 평문이 그대로 저장되지 않는다 — 인코더가 반환한 값이 저장됨
            assertThat(saved.getPassword()).isEqualTo("HASHED");
            assertThat(saved.getPassword()).isNotEqualTo("password123");
        }

        @Test
        @DisplayName("이메일이 이미 있으면 DuplicateEmailException 을 던지고 save 는 호출하지 않는다")
        void duplicateEmail() {
            given(repository.existsByEmail("a@b.com")).willReturn(true);

            assertThatThrownBy(() -> userService.signup(UserSignupRequestDto.builder()
                    .email("a@b.com").password("password123").nickname("nick").build()))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("a@b.com");

            verify(repository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("닉네임이 이미 있으면 DuplicateNicknameException 을 던지고 save 는 호출하지 않는다")
        void duplicateNickname() {
            given(repository.existsByEmail("a@b.com")).willReturn(false);
            given(repository.existsByNickname("nick")).willReturn(true);

            assertThatThrownBy(() -> userService.signup(UserSignupRequestDto.builder()
                    .email("a@b.com").password("password123").nickname("nick").build()))
                    .isInstanceOf(DuplicateNicknameException.class)
                    .hasMessageContaining("nick");

            verify(repository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(any());
        }
    }

    @Nested
    @DisplayName("login — 로그인")
    class Login {

        @Test
        @DisplayName("이메일/비밀번호 정상이면 JWT access token + userId 를 반환한다")
        void success() {
            UUID userId = UUID.randomUUID();
            User stored = userWithId(userId, "a@b.com", "HASHED", "nick");
            given(repository.findByEmail("a@b.com")).willReturn(Optional.of(stored));
            given(passwordEncoder.matches("password123", "HASHED")).willReturn(true);
            given(jwtTokenProvider.createAccessToken(userId)).willReturn("issued.jwt.token");

            LoginResponseDto result = userService.login(LoginRequestDto.builder()
                    .email("a@b.com").password("password123").build());

            assertThat(result.getAccessToken()).isEqualTo("issued.jwt.token");
            assertThat(result.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 InvalidCredentialsException (열거 공격 방지 — 메시지 동일)")
        void emailNotFound() {
            given(repository.findByEmail("a@b.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(LoginRequestDto.builder()
                    .email("a@b.com").password("password123").build()))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");

            verify(passwordEncoder, never()).matches(any(), any());
            verify(jwtTokenProvider, never()).createAccessToken(any());
        }

        @Test
        @DisplayName("비밀번호가 불일치하면 InvalidCredentialsException 을 던지고 토큰을 발급하지 않는다")
        void passwordMismatch() {
            User stored = userWithId(UUID.randomUUID(), "a@b.com", "HASHED", "nick");
            given(repository.findByEmail("a@b.com")).willReturn(Optional.of(stored));
            given(passwordEncoder.matches("wrong", "HASHED")).willReturn(false);

            assertThatThrownBy(() -> userService.login(LoginRequestDto.builder()
                    .email("a@b.com").password("wrong").build()))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");

            verify(jwtTokenProvider, never()).createAccessToken(any());
        }
    }
}
