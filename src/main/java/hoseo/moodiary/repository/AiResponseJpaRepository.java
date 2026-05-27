package hoseo.moodiary.repository;

import hoseo.moodiary.entitiy.AiResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiResponseJpaRepository extends JpaRepository<AiResponse, UUID> {

    /**
     * 특정 게시글의 AI 응답 1개 (1:1 관계). 폴링 엔드포인트와 Async 워커 양쪽에서 사용.
     * {@code ai_response.post_id} 의 unique 인덱스로 단일 row.
     */
    Optional<AiResponse> findByPost_Id(UUID postId);

    /**
     * 게시글 삭제 시 AI 응답도 함께 제거. Spring Data 의 derived delete 라 자동 {@code @Modifying} + 트랜잭션 필요.
     * {@code PostService.delete()} 가 자기 트랜잭션 안에서 호출.
     */
    void deleteByPost_Id(UUID postId);
}
