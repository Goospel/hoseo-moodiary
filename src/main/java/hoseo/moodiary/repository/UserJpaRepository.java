package hoseo.moodiary.repository;

import hoseo.moodiary.entitiy.AuthProvider;
import hoseo.moodiary.entitiy.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    /** PR 2-c(JWT 로그인)에서 사용. */
    Optional<User> findByEmail(String email);

    /**
     * PR 12-pre — OAuth2 로그인. (provider, providerId) 조합으로 기존 사용자 찾기.
     * LOCAL 회원은 providerId 가 null 이라 이 메서드로 검색되지 않는다.
     */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
