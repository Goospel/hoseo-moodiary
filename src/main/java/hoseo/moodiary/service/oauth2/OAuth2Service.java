package hoseo.moodiary.service.oauth2;

import hoseo.moodiary.dto.response.LoginResponseDto;
import hoseo.moodiary.entitiy.AuthProvider;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.EmailAlreadyExistsForOtherProviderException;
import hoseo.moodiary.exception.OAuth2VerificationException;
import hoseo.moodiary.repository.UserJpaRepository;
import hoseo.moodiary.security.JwtTokenProvider;
import hoseo.moodiary.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * OAuth2 로그인 / 가입 서비스.
 *
 * <p>흐름:
 * <ol>
 *   <li>{@link OAuth2Provider#verifyAndExtract(String)} 로 provider 측 access token 검증 + user info 추출.</li>
 *   <li>{@code (provider, providerId)} 로 기존 사용자 조회.</li>
 *   <li>있으면 → access + refresh 발급해서 로그인 응답.</li>
 *   <li>없으면 → 신규 가입 흐름:
 *     <ul>
 *       <li>이메일이 이미 다른 provider 로 가입돼 있으면 → {@link EmailAlreadyExistsForOtherProviderException} (409).</li>
 *       <li>닉네임 충돌 시 suffix 부여 (e.g. {@code "alice-google-12ab"}) — 사용자 식별 가능한 짧은 형태.</li>
 *       <li>{@link User#createOAuth2(AuthProvider, String, String, String)} 으로 저장.</li>
 *       <li>access + refresh 발급.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>응답은 LOCAL 로그인과 동일한 {@link LoginResponseDto} — FE 는 access token 을 같은 방식으로 사용.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OAuth2Service {

    private static final int NICKNAME_MAX_LENGTH = 20;

    private final UserJpaRepository userRepository;
    private final OAuth2Provider oauth2Provider;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * OAuth2 로그인 / 가입.
     *
     * @param provider             path 의 provider (현재 GOOGLE 만). LOCAL 은 reject.
     * @param providerAccessToken  provider 측 token (Stub 모드는 stub:... 형식).
     * @return access + refresh + userId
     */
    public LoginResponseDto login(AuthProvider provider, String providerAccessToken) {
        if (provider == AuthProvider.LOCAL) {
            // /auth/oauth2/LOCAL 는 의미 없음 — 401 로 응답 (provider 검증 실패 카테고리).
            throw new OAuth2VerificationException("LOCAL provider 는 OAuth2 endpoint 로 지원하지 않음");
        }

        // 1. provider 측 검증 + user info 추출
        OAuth2UserInfo info = oauth2Provider.verifyAndExtract(providerAccessToken);

        // 2. (provider, providerId) 로 기존 user 조회
        User user = userRepository.findByProviderAndProviderId(provider, info.providerId())
                .orElseGet(() -> registerNew(provider, info));

        // 3. access + refresh 발급
        UUID userId = user.getId();
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = refreshTokenService.issue(userId);
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .build();
    }

    /**
     * 신규 OAuth2 사용자 등록.
     *
     * <p>이메일 중복 정책: 다른 provider 로 이미 가입된 이메일이면 차단 (LOCAL + OAuth2 한 사용자만).
     * 닉네임 충돌 시 suffix 로 회피.
     */
    private User registerNew(AuthProvider provider, OAuth2UserInfo info) {
        userRepository.findByEmail(info.email())
                .ifPresent(existing -> {
                    throw new EmailAlreadyExistsForOtherProviderException(info.email(), existing.getProvider());
                });

        String nickname = resolveUniqueNickname(info.nickname(), info.providerId());
        User user = User.createOAuth2(provider, info.providerId(), info.email(), nickname);
        return userRepository.save(user);
    }

    /**
     * 닉네임 길이 / 중복 처리.
     *
     * <p>전략:
     * <ul>
     *   <li>provider 측 닉네임이 20자 초과면 truncate.</li>
     *   <li>이미 다른 사용자가 그 닉네임 사용 중이면 suffix 부여 ({@code original-{providerId의 앞 4자}}).</li>
     *   <li>suffix 후에도 충돌하면 (희박) — UUID prefix 폴백 (운영에서 거의 없음, 사후 모니터링 가치).</li>
     * </ul>
     */
    private String resolveUniqueNickname(String rawNickname, String providerId) {
        String candidate = rawNickname.length() > NICKNAME_MAX_LENGTH
                ? rawNickname.substring(0, NICKNAME_MAX_LENGTH)
                : rawNickname;

        if (!userRepository.existsByNickname(candidate)) {
            return candidate;
        }

        // suffix: 원본 일부 + providerId 앞 4자 — 항상 20자 이내로 보장.
        String suffix = "-" + providerId.substring(0, Math.min(4, providerId.length()));
        int baseLen = Math.min(candidate.length(), NICKNAME_MAX_LENGTH - suffix.length());
        String withSuffix = candidate.substring(0, baseLen) + suffix;

        if (!userRepository.existsByNickname(withSuffix)) {
            return withSuffix;
        }

        // 최종 폴백 — UUID 앞 8자. 충돌 확률 ~ 0.
        return "u-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
