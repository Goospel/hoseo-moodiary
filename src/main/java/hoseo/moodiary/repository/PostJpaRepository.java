package hoseo.moodiary.repository;

import hoseo.moodiary.entitiy.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 게시글 기본 CRUD. 단건 조회/저장/삭제는 여기, 목록 조회(정렬·필터)는 {@code PostSearchRepository}(QueryDSL).
 */
public interface PostJpaRepository extends JpaRepository<Post, UUID> {
}
