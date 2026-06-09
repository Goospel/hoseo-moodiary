package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.request.PostSortField;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.entitiy.AiResponse;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.InvalidPostSearchException;
import hoseo.moodiary.exception.PostAccessDeniedException;
import hoseo.moodiary.exception.PostNotFoundException;
import hoseo.moodiary.repository.AiResponseJpaRepository;
import hoseo.moodiary.repository.PostJpaRepository;
import hoseo.moodiary.repository.PostSearchRepository;
import hoseo.moodiary.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    /** sort 파라미터 기본값 — postDate 내림차순 (최근 일기가 위로). */
    private static final PostSortField DEFAULT_SORT_FIELD = PostSortField.POST_DATE;
    private static final boolean DEFAULT_ASCENDING = false;

    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;
    private final AiResponseJpaRepository aiResponseRepository;
    private final PostSearchRepository postSearchRepository;

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

    /**
     * 본인 게시글 목록을 정렬 + 선택적 필터(날짜 범위 / 키워드)로 조회.
     *
     * @param from    postDate 하한 (inclusive, null 이면 무제한)
     * @param to      postDate 상한 (inclusive, null 이면 무제한)
     * @param keyword 제목/내용 부분일치 (null/blank 이면 무시)
     * @param sort    {@code "필드,방향"} 형식 (예: {@code "postDate,desc"}). null/blank 이면 기본값(postDate desc).
     *                필드는 화이트리스트({@link PostSortField})만, 방향은 asc/desc 만 허용 — 아니면 400.
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts(UUID currentUserId,
                                             LocalDate from,
                                             LocalDate to,
                                             String keyword,
                                             String sort) {
        validateRange(from, to);
        SortSpec sortSpec = parseSort(sort);
        return postSearchRepository
                .search(currentUserId, from, to, keyword, sortSpec.field(), sortSpec.ascending())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** from > to 는 빈 결과를 줄 게 뻔한 사용자 실수 — 조용히 빈 배열 대신 400 으로 알려준다. */
    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidPostSearchException(
                    "조회 시작일(from)이 종료일(to)보다 늦습니다. from=" + from + ", to=" + to);
        }
    }

    /** {@code "필드,방향"} 파싱. null/blank → 기본값. 화이트리스트/방향 위반 → 400. */
    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec(DEFAULT_SORT_FIELD, DEFAULT_ASCENDING);
        }
        String[] parts = sort.split(",");
        PostSortField field = PostSortField.from(parts[0].trim());
        boolean ascending = parts.length >= 2 ? parseDirection(parts[1].trim()) : DEFAULT_ASCENDING;
        return new SortSpec(field, ascending);
    }

    private boolean parseDirection(String direction) {
        if ("asc".equalsIgnoreCase(direction)) {
            return true;
        }
        if ("desc".equalsIgnoreCase(direction)) {
            return false;
        }
        throw new InvalidPostSearchException(
                "지원하지 않는 정렬 방향입니다: " + direction + " (가능: asc, desc)");
    }

    private record SortSpec(PostSortField field, boolean ascending) {
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPost(UUID currentUserId, UUID postId) {
        return toResponse(loadOwned(currentUserId, postId));
    }

    public PostResponseDto update(UUID currentUserId, UUID postId, PostRequestDto requestDto) {
        Post post = loadOwned(currentUserId, postId);
        // postDate null 폴백은 create 와 일관 — 누락 시 오늘.
        LocalDate postDate = requestDto.getPostDate() != null ? requestDto.getPostDate() : LocalDate.now();
        post.update(requestDto.getTitle(), requestDto.getContent(), postDate);
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
                .postDate(post.getPostDate())
                .build();
    }
}
