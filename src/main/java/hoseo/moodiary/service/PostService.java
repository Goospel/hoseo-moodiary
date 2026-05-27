package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.entitiy.AiResponse;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.PostAccessDeniedException;
import hoseo.moodiary.exception.PostNotFoundException;
import hoseo.moodiary.repository.AiResponseJpaRepository;
import hoseo.moodiary.repository.PostJpaRepository;
import hoseo.moodiary.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Post 비즈니스 로직.
 *
 * <p>모든 메서드는 현재 인증된 사용자 ID({@code currentUserId})를 받는다 — 컨트롤러에서
 * {@code @AuthenticationPrincipal UUID userId}로 받아 그대로 넘어온다.
 *
 * <p>소유권 규칙:
 * <ul>
 *   <li>{@code create} — 현재 사용자로 자동 채움</li>
 *   <li>{@code getAllPosts} — 본인 글만 반환</li>
 *   <li>{@code getPost} — 본인 글이 아니면 {@link PostAccessDeniedException} (403)</li>
 *   <li>{@code update}, {@code delete} — 마찬가지로 본인 글만</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;
    private final AiResponseJpaRepository aiResponseRepository;

    /**
     * 일기 + AI 응답 PENDING row 를 같은 트랜잭션에 저장. 이 메서드 return 후 컨트롤러가
     * {@code AiResponseService.triggerAsync(postId)} 를 호출해 비동기 추론을 트리거한다.
     *
     * <p>같은 트랜잭션 정책의 이유: 일기는 저장됐는데 PENDING row 가 없는 상태 (= 폴링 시 404) 를
     * 방지. 두 row 의 invariant 를 트랜잭션 경계로 보장.
     */
    public UUID create(UUID currentUserId, PostRequestDto requestDto) {
        // getReferenceById: 프록시 반환 — User 테이블 SELECT 발생하지 않음. FK 채우기에 충분.
        User userRef = userRepository.getReferenceById(currentUserId);
        Post saved = postRepository.save(requestDto.toEntity(userRef));
        aiResponseRepository.save(AiResponse.builder().post(saved).build());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts(UUID currentUserId) {
        return postRepository.findAllByUser_Id(currentUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPost(UUID currentUserId, UUID postId) {
        return toResponse(loadOwned(currentUserId, postId));
    }

    public PostResponseDto update(UUID currentUserId, UUID postId, PostRequestDto requestDto) {
        Post post = loadOwned(currentUserId, postId);
        post.update(requestDto.getTitle(), requestDto.getContent());
        return toResponse(post);
    }

    public void delete(UUID currentUserId, UUID postId) {
        Post post = loadOwned(currentUserId, postId);
        // 1:1 AiResponse 도 함께 제거 — Post FK 가 NOT NULL 이라 순서 중요 (자식 먼저, 부모 나중).
        aiResponseRepository.deleteByPost_Id(postId);
        postRepository.delete(post);
    }

    /** 존재 + 소유 동시 검증. 없으면 404, 남의 글이면 403. */
    private Post loadOwned(UUID currentUserId, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        if (!post.isOwnedBy(currentUserId)) {
            throw new PostAccessDeniedException(postId);
        }
        return post;
    }

    private PostResponseDto toResponse(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .build();
    }
}
