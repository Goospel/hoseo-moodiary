package hoseo.moodiary.repository;

import hoseo.moodiary.entitiy.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostJpaRepository extends JpaRepository<Post, UUID> {

    /** 특정 사용자의 모든 게시글. {@code post(user_id, created_at)} 인덱스 권장 (PR 5 캘린더에서 더 필요). */
    List<Post> findAllByUser_Id(UUID userId);
}
