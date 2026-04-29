package hoseo.moodiary.repository;

import hoseo.moodiary.entitiy.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostJpaRepository extends JpaRepository<Post, Long> {
}
