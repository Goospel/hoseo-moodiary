package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.UserSignupRequestDto;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.DuplicateEmailException;
import hoseo.moodiary.exception.DuplicateNicknameException;
import hoseo.moodiary.repository.UserJpaRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserService 단위 테스트 — 회원가입 분기 + 비밀번호 해싱 위임.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserJpaRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
}
