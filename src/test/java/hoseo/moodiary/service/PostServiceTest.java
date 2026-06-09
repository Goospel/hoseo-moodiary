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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PostService 단위 테스트.
 *
 * <p>PR 3 이후 — 모든 메서드가 현재 사용자 ID를 받는다. 본인 글이 아닌 경우 {@link PostAccessDeniedException}.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostJpaRepository postRepository;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private AiResponseJpaRepository aiResponseRepository;

    @Mock
    private PostSearchRepository postSearchRepository;

    @InjectMocks
    private PostService postService;

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static User userWithId(UUID id) {
        User user = User.createLocal("a@b.com", "HASHED", "nick");
        setId(user, "id", id);
        return user;
    }

    private static Post postWithId(UUID postId, UUID ownerId, String title, String content) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .postDate(LocalDate.now())
                .user(userWithId(ownerId))
                .build();
        setId(post, "id", postId);
        return post;
    }

    private static void setId(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    @DisplayName("create — 게시글 생성")
    class Create {

        @Test
        @DisplayName("현재 사용자를 작성자로 채워 저장하고, 생성된 ID를 반환한다")
        void success() {
            UUID newPostId = UUID.randomUUID();
            given(userRepository.getReferenceById(OWNER_ID)).willReturn(userWithId(OWNER_ID));
            given(postRepository.save(any(Post.class)))
                    .willReturn(postWithId(newPostId, OWNER_ID, "t", "c"));

            UUID result = postService.create(OWNER_ID,
                    PostRequestDto.builder().title("t").content("c").build());

            assertThat(result).isEqualTo(newPostId);
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("같은 트랜잭션에 AiResponse(PENDING) row 도 저장한다 — 일기와 PENDING 의 정합성 보장")
        void alsoCreatesPendingAiResponse() {
            UUID newPostId = UUID.randomUUID();
            given(userRepository.getReferenceById(OWNER_ID)).willReturn(userWithId(OWNER_ID));
            given(postRepository.save(any(Post.class)))
                    .willReturn(postWithId(newPostId, OWNER_ID, "t", "c"));

            postService.create(OWNER_ID,
                    PostRequestDto.builder().title("t").content("c").build());

            verify(aiResponseRepository).save(any(AiResponse.class));
        }

        @Test
        @DisplayName("postDate 명시 시 그대로 엔티티에 들어간다 — 지나간 날짜 일기")
        void postDateExplicit() {
            UUID newPostId = UUID.randomUUID();
            LocalDate yesterday = LocalDate.now().minusDays(1);
            given(userRepository.getReferenceById(OWNER_ID)).willReturn(userWithId(OWNER_ID));
            given(postRepository.save(any(Post.class)))
                    .willReturn(postWithId(newPostId, OWNER_ID, "t", "c"));

            postService.create(OWNER_ID,
                    PostRequestDto.builder().title("t").content("c").postDate(yesterday).build());

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertThat(captor.getValue().getPostDate()).isEqualTo(yesterday);
        }

        @Test
        @DisplayName("postDate 누락 시 today 로 폴백 — 기본 동작 '오늘 일기'")
        void postDateOmittedFallsBackToToday() {
            UUID newPostId = UUID.randomUUID();
            given(userRepository.getReferenceById(OWNER_ID)).willReturn(userWithId(OWNER_ID));
            given(postRepository.save(any(Post.class)))
                    .willReturn(postWithId(newPostId, OWNER_ID, "t", "c"));

            postService.create(OWNER_ID,
                    PostRequestDto.builder().title("t").content("c").build());

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertThat(captor.getValue().getPostDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("getAllPosts — 본인 글 목록 (정렬 + 필터)")
    class GetAllPosts {

        @Test
        @DisplayName("결과가 없으면 빈 리스트")
        void empty() {
            given(postSearchRepository.search(eq(OWNER_ID), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(List.of());

            assertThat(postService.getAllPosts(OWNER_ID, null, null, null, null)).isEmpty();
        }

        @Test
        @DisplayName("조회된 글을 DTO 로 매핑해 반환한다")
        void withItems() {
            UUID p1 = UUID.randomUUID();
            UUID p2 = UUID.randomUUID();
            given(postSearchRepository.search(eq(OWNER_ID), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(List.of(
                            postWithId(p1, OWNER_ID, "t1", "c1"),
                            postWithId(p2, OWNER_ID, "t2", "c2")
                    ));

            List<PostResponseDto> result = postService.getAllPosts(OWNER_ID, null, null, null, null);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(PostResponseDto::getId).containsExactly(p1, p2);
        }

        @Test
        @DisplayName("sort/필터 누락 시 기본값(postDate desc) 으로 repository 호출")
        void defaultSort() {
            given(postSearchRepository.search(eq(OWNER_ID), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(List.of());

            postService.getAllPosts(OWNER_ID, null, null, null, null);

            verify(postSearchRepository).search(OWNER_ID, null, null, null, PostSortField.POST_DATE, false);
        }

        @Test
        @DisplayName("from/to/keyword/sort 를 그대로 파싱해 repository 에 전달한다")
        void passesFiltersThrough() {
            LocalDate from = LocalDate.of(2026, 5, 1);
            LocalDate to = LocalDate.of(2026, 5, 31);
            given(postSearchRepository.search(eq(OWNER_ID), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(List.of());

            postService.getAllPosts(OWNER_ID, from, to, "여행", "createdAt,asc");

            verify(postSearchRepository).search(OWNER_ID, from, to, "여행", PostSortField.CREATED_AT, true);
        }

        @Test
        @DisplayName("방향 생략 시 desc 기본 — 'createdAt' 만 줘도 동작")
        void directionOmittedDefaultsDesc() {
            given(postSearchRepository.search(eq(OWNER_ID), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(List.of());

            postService.getAllPosts(OWNER_ID, null, null, null, "createdAt");

            verify(postSearchRepository).search(OWNER_ID, null, null, null, PostSortField.CREATED_AT, false);
        }

        @Test
        @DisplayName("화이트리스트에 없는 정렬 필드 → InvalidPostSearchException, repository 미호출")
        void invalidSortField() {
            assertThatThrownBy(() -> postService.getAllPosts(OWNER_ID, null, null, null, "content,desc"))
                    .isInstanceOf(InvalidPostSearchException.class);

            verify(postSearchRepository, never()).search(any(), any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("알 수 없는 정렬 방향 → InvalidPostSearchException")
        void invalidSortDirection() {
            assertThatThrownBy(() -> postService.getAllPosts(OWNER_ID, null, null, null, "postDate,sideways"))
                    .isInstanceOf(InvalidPostSearchException.class);

            verify(postSearchRepository, never()).search(any(), any(), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("from 이 to 보다 늦으면 InvalidPostSearchException, repository 미호출")
        void invertedRange() {
            LocalDate from = LocalDate.of(2026, 5, 31);
            LocalDate to = LocalDate.of(2026, 5, 1);

            assertThatThrownBy(() -> postService.getAllPosts(OWNER_ID, from, to, null, null))
                    .isInstanceOf(InvalidPostSearchException.class);

            verify(postSearchRepository, never()).search(any(), any(), any(), any(), any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("getPost — 단건 조회")
    class GetPost {

        @Test
        @DisplayName("본인 글이면 DTO 를 반환한다")
        void ownedSuccess() {
            UUID postId = UUID.randomUUID();
            given(postRepository.findById(postId))
                    .willReturn(Optional.of(postWithId(postId, OWNER_ID, "t", "c")));

            PostResponseDto result = postService.getPost(OWNER_ID, postId);

            assertThat(result.getId()).isEqualTo(postId);
            assertThat(result.getTitle()).isEqualTo("t");
        }

        @Test
        @DisplayName("존재하지 않으면 PostNotFoundException")
        void notFound() {
            UUID postId = UUID.randomUUID();
            given(postRepository.findById(postId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPost(OWNER_ID, postId))
                    .isInstanceOf(PostNotFoundException.class);
        }

        @Test
        @DisplayName("타인 글이면 PostAccessDeniedException")
        void notOwned() {
            UUID postId = UUID.randomUUID();
            given(postRepository.findById(postId))
                    .willReturn(Optional.of(postWithId(postId, OTHER_USER_ID, "t", "c")));

            assertThatThrownBy(() -> postService.getPost(OWNER_ID, postId))
                    .isInstanceOf(PostAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("update — 수정")
    class Update {

        @Test
        @DisplayName("본인 글이면 update(...) 로 필드를 갱신하고 갱신된 DTO 를 반환한다")
        void ownedSuccess() {
            UUID postId = UUID.randomUUID();
            Post existing = postWithId(postId, OWNER_ID, "old", "old");
            given(postRepository.findById(postId)).willReturn(Optional.of(existing));

            PostResponseDto result = postService.update(OWNER_ID, postId,
                    PostRequestDto.builder().title("newT").content("newC").build());

            assertThat(existing.getTitle()).isEqualTo("newT");
            assertThat(existing.getContent()).isEqualTo("newC");
            assertThat(result.getTitle()).isEqualTo("newT");
            verify(postRepository, never()).save(any(Post.class));
        }

        @Test
        @DisplayName("postDate 명시 시 엔티티의 postDate 도 갱신된다 — 날짜 잘못 적었을 때 수정")
        void postDateUpdated() {
            UUID postId = UUID.randomUUID();
            Post existing = postWithId(postId, OWNER_ID, "old", "old");
            LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
            given(postRepository.findById(postId)).willReturn(Optional.of(existing));

            PostResponseDto result = postService.update(OWNER_ID, postId,
                    PostRequestDto.builder().title("newT").content("newC").postDate(twoDaysAgo).build());

            assertThat(existing.getPostDate()).isEqualTo(twoDaysAgo);
            assertThat(result.getPostDate()).isEqualTo(twoDaysAgo);
        }

        @Test
        @DisplayName("postDate 누락 시 update 도 today 로 폴백 — create 와 일관")
        void postDateOmittedFallsBackToToday() {
            UUID postId = UUID.randomUUID();
            Post existing = postWithId(postId, OWNER_ID, "old", "old");
            given(postRepository.findById(postId)).willReturn(Optional.of(existing));

            postService.update(OWNER_ID, postId,
                    PostRequestDto.builder().title("newT").content("newC").build());

            assertThat(existing.getPostDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("존재하지 않으면 PostNotFoundException")
        void notFound() {
            UUID postId = UUID.randomUUID();
            given(postRepository.findById(postId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.update(OWNER_ID, postId,
                    PostRequestDto.builder().title("t").content("c").build()))
                    .isInstanceOf(PostNotFoundException.class);
        }

        @Test
        @DisplayName("타인 글이면 PostAccessDeniedException 을 던지고 필드를 갱신하지 않는다")
        void notOwned() {
            UUID postId = UUID.randomUUID();
            Post existing = postWithId(postId, OTHER_USER_ID, "old", "old");
            given(postRepository.findById(postId)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> postService.update(OWNER_ID, postId,
                    PostRequestDto.builder().title("newT").content("newC").build()))
                    .isInstanceOf(PostAccessDeniedException.class);

            assertThat(existing.getTitle()).isEqualTo("old");
        }
    }

    @Nested
    @DisplayName("delete — 삭제")
    class Delete {

        @Test
        @DisplayName("본인 글이면 AiResponse cascade 삭제 후 Post 삭제")
        void ownedSuccess() {
            UUID postId = UUID.randomUUID();
            Post existing = postWithId(postId, OWNER_ID, "t", "c");
            given(postRepository.findById(postId)).willReturn(Optional.of(existing));

            postService.delete(OWNER_ID, postId);

            // 자식 (AiResponse) 먼저, 부모 (Post) 나중 — FK NOT NULL 정합성.
            verify(aiResponseRepository).deleteByPost_Id(postId);
            verify(postRepository).delete(existing);
        }

        @Test
        @DisplayName("존재하지 않으면 PostNotFoundException 을 던지고 delete 미호출")
        void notFound() {
            UUID postId = UUID.randomUUID();
            given(postRepository.findById(postId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.delete(OWNER_ID, postId))
                    .isInstanceOf(PostNotFoundException.class);

            verify(postRepository, never()).delete(any(Post.class));
        }

        @Test
        @DisplayName("타인 글이면 PostAccessDeniedException 을 던지고 delete 미호출")
        void notOwned() {
            UUID postId = UUID.randomUUID();
            Post existing = postWithId(postId, OTHER_USER_ID, "t", "c");
            given(postRepository.findById(postId)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> postService.delete(OWNER_ID, postId))
                    .isInstanceOf(PostAccessDeniedException.class);

            verify(postRepository, never()).delete(any(Post.class));
        }
    }
}
