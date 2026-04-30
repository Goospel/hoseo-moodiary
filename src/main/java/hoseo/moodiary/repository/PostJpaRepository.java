package hoseo.moodiary.repository;

import hoseo.moodiary.entitiy.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostJpaRepository extends JpaRepository<Post, UUID> {
}
