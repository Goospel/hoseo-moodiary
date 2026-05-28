package hoseo.moodiary.service.oauth2;

import hoseo.moodiary.dto.response.LoginResponseDto;
import hoseo.moodiary.entitiy.AuthProvider;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.EmailAlreadyExistsForOtherProviderException;
import hoseo.moodiary.exception.OAuth2VerificationException;
import hoseo.moodiary.repository.UserJpaRepository;
import hoseo.moodiary.security.JwtTokenProvider;
import hoseo.moodiary.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * OAuth2Service 단위 테스트 — PR 12-pre.
 *
 * <p>커버 흐름:
 * <ul>
 *   <li>신규 가입 (이메일 신규) → access + refresh 발급</li>
 *   <li>기존 OAuth2 사용자 재로그인 → 새 INSERT 없이 토큰만 발급</li>
 *   <li>이메일이 다른 provider 로 이미 가입됨 → 409 (EmailAlreadyExistsForOtherProviderException)</li>
 *   <li>닉네임 충돌 → suffix 부여</li>
 *   <li>LOCAL provider 로 호출 → OAuth2VerificationException</li>
 *   <li>Provider 가 토큰 reject → OAuth2VerificationException 그대로 전파</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OAuth2ServiceTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private OAuth2Provider oauth2Provider;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private OAuth2Service oauth2Service;

    private static User savedUserWithId(UUID id, AuthProvider provider, String providerId, String email, String nickname) {
        User user = User.createOAuth2(provider, providerId, email, nickname);
        try {
            Field f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    @Nested
    @DisplayName("신규 가입 + 로그인")
    class NewSignup {

        @Test
        @DisplayName("이메일 신규 + (provider, providerId) 신규 → User INSERT + access/refresh 발급")
        void newUser_savesAndIssuesTokens() {
            String accessToken = "stub:GOOGLE:google-123:alice@example.com:Alice";
            OAuth2UserInfo info = new OAuth2UserInfo("google-123", "alice@example.com", "Alice");
            UUID newId = UUID.randomUUID();
            User savedUser = savedUserWithId(newId, AuthProvider.GOOGLE, "google-123", "alice@example.com", "Alice");

            given(oauth2Provider.verifyAndExtract(accessToken)).willReturn(info);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123")).willReturn(Optional.empty());
            given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.empty());
            given(userRepository.existsByNickname("Alice")).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(jwtTokenProvider.createAccessToken(newId)).willReturn("ACCESS_JWT");
            given(refreshTokenService.issue(newId)).willReturn("RAW_REFRESH");

            LoginResponseDto response = oauth2Service.login(AuthProvider.GOOGLE, accessToken);

            assertThat(response.getUserId()).isEqualTo(newId);
            assertThat(response.getAccessToken()).isEqualTo("ACCESS_JWT");
            assertThat(response.getRefreshToken()).isEqualTo("RAW_REFRESH");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(saved.getProviderId()).isEqualTo("google-123");
            assertThat(saved.getEmail()).isEqualTo("alice@example.com");
            assertThat(saved.getNickname()).isEqualTo("Alice");
            assertThat(saved.getPassword()).isNull();
        }

        @Test
        @DisplayName("닉네임 충돌 → suffix '-{providerId 앞 4자}' 부여")
        void nicknameCollision_appendsSuffix() {
            // providerId 앞 4자 = "goog" — suffix 형성 검증.
            String accessToken = "stub:GOOGLE:google-987:bob@example.com:Bob";
            OAuth2UserInfo info = new OAuth2UserInfo("google-987", "bob@example.com", "Bob");
            given(oauth2Provider.verifyAndExtract(accessToken)).willReturn(info);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-987")).willReturn(Optional.empty());
            given(userRepository.findByEmail("bob@example.com")).willReturn(Optional.empty());
            given(userRepository.existsByNickname("Bob")).willReturn(true);          // 충돌
            given(userRepository.existsByNickname("Bob-goog")).willReturn(false);    // suffix 후 OK
            UUID newId = UUID.randomUUID();
            given(userRepository.save(any(User.class))).willReturn(savedUserWithId(newId, AuthProvider.GOOGLE, "google-987", "bob@example.com", "Bob-goog"));
            given(jwtTokenProvider.createAccessToken(newId)).willReturn("ACC");
            given(refreshTokenService.issue(newId)).willReturn("REF");

            oauth2Service.login(AuthProvider.GOOGLE, accessToken);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getNickname()).isEqualTo("Bob-goog");
        }
    }

    @Nested
    @DisplayName("기존 사용자 재로그인")
    class ExistingUser {

        @Test
        @DisplayName("(provider, providerId) 매칭 → INSERT 없이 토큰만 발급")
        void existingUser_skipsInsert() {
            String accessToken = "stub:GOOGLE:google-123:alice@example.com:Alice";
            OAuth2UserInfo info = new OAuth2UserInfo("google-123", "alice@example.com", "Alice");
            UUID existingId = UUID.randomUUID();
            User existing = savedUserWithId(existingId, AuthProvider.GOOGLE, "google-123", "alice@example.com", "Alice");

            given(oauth2Provider.verifyAndExtract(accessToken)).willReturn(info);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123")).willReturn(Optional.of(existing));
            given(jwtTokenProvider.createAccessToken(existingId)).willReturn("ACC");
            given(refreshTokenService.issue(existingId)).willReturn("REF");

            LoginResponseDto response = oauth2Service.login(AuthProvider.GOOGLE, accessToken);

            assertThat(response.getUserId()).isEqualTo(existingId);
            verify(userRepository, never()).save(any(User.class));
            verify(userRepository, never()).findByEmail(any());
        }
    }

    @Nested
    @DisplayName("이메일 중복 — 다른 provider 와 충돌")
    class EmailCollision {

        @Test
        @DisplayName("이메일이 이미 LOCAL 로 가입 → 409 EmailAlreadyExistsForOtherProviderException")
        void emailAlreadyLocal_throws409() {
            String accessToken = "stub:GOOGLE:google-123:alice@example.com:Alice";
            OAuth2UserInfo info = new OAuth2UserInfo("google-123", "alice@example.com", "Alice");
            // LOCAL user 생성 — createLocal factory 사용 (createOAuth2 는 LOCAL 거부).
            User localUser = User.createLocal("alice@example.com", "BCRYPT_HASH", "AliceLocal");
            try {
                Field f = User.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(localUser, UUID.randomUUID());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            given(oauth2Provider.verifyAndExtract(accessToken)).willReturn(info);
            given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123")).willReturn(Optional.empty());
            given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(localUser));

            assertThatThrownBy(() -> oauth2Service.login(AuthProvider.GOOGLE, accessToken))
                    .isInstanceOf(EmailAlreadyExistsForOtherProviderException.class)
                    .hasMessageContaining("LOCAL");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Provider 검증 / 입력 검증")
    class Validation {

        @Test
        @DisplayName("LOCAL provider 로 OAuth2 endpoint 호출 → OAuth2VerificationException")
        void localProviderRejected() {
            assertThatThrownBy(() -> oauth2Service.login(AuthProvider.LOCAL, "anything"))
                    .isInstanceOf(OAuth2VerificationException.class)
                    .hasMessageContaining("LOCAL");

            verify(oauth2Provider, never()).verifyAndExtract(any());
        }

        @Test
        @DisplayName("Provider 가 토큰 reject → OAuth2VerificationException 그대로 전파")
        void providerRejection_propagates() {
            String accessToken = "invalid-token";
            given(oauth2Provider.verifyAndExtract(accessToken))
                    .willThrow(new OAuth2VerificationException("Stub 토큰 형식 아님"));

            assertThatThrownBy(() -> oauth2Service.login(AuthProvider.GOOGLE, accessToken))
                    .isInstanceOf(OAuth2VerificationException.class);

            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).issue(any());
        }
    }
}
