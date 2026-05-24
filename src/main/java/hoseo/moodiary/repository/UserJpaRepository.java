package hoseo.moodiary.repository;

import hoseo.moodiary.entitiy.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    /** PR 2-c(JWT 로그인)에서 사용. */
    Optional<User> findByEmail(String email);
}
