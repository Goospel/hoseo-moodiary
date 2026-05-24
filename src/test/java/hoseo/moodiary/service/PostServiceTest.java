package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.exception.PostNotFoundException;
import hoseo.moodiary.repository.PostJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PostService 순수 단위 테스트.
 *
 * <p>Spring 컨텍스트를 띄우지 않고({@link MockitoExtension}만 사용) Repository를 Mockito로 가짜로 주입한다.
 * 트랜잭션/JPA dirty-checking은 통합 테스트의 영역이고, 여기서는
 * <ul>
 *   <li>분기 로직 — not-found 시 {@link PostNotFoundException} 발사 여부</li>
 *   <li>도메인 메서드 호출 — {@code update}가 엔티티의 {@link Post#update} 를 거치는지(=dirty checking 전제)</li>
 *   <li>Repository 호출 사이드이펙트 — {@code delete}가 not-found에서 {@code deleteById}를 호출하지 않는지</li>
 * </ul>
 * 만 격리해서 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostJpaRepository repository;

    @InjectMocks
    private PostService postService;

    /**
     * {@code Post.id}는 {@code @UuidGenerator}로 영속화 시점에 자동 생성되므로
     * Mockito 환경에서는 비어 있다. 단위 테스트에서 식별자를 미리 박아두고 비교해야 할 때
     * 리플렉션으로 강제 주입한다.
     */
    private static Post postWithId(UUID id, String title, String content) {
        Post post = Post.builder().title(title).content(content).build();
        try {
            Field f = Post.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(post, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return post;
    }

    @Nested
    @DisplayName("create — 게시글 생성")
    class Create {

        @Test
        @DisplayName("Repository에 위임하고 저장된 엔티티의 ID를 그대로 반환한다")
        void create_returnsSavedId() {
            UUID generatedId = UUID.randomUUID();
            Post saved = postWithId(generatedId, "t", "c");
            given(repository.save(any(Post.class))).willReturn(saved);

            UUID result = postService.create(
                    PostRequestDto.builder().title("t").content("c").build());

            assertThat(result).isEqualTo(generatedId);
            verify(repository).save(any(Post.class));
        }
    }

    @Nested
    @DisplayName("getAllPosts — 전체 조회")
    class GetAllPosts {

        @Test
        @DisplayName("저장된 글이 없으면 빈 리스트를 반환한다")
        void empty() {
            given(repository.findAll()).willReturn(List.of());

            List<PostResponseDto> result = postService.getAllPosts();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("저장된 글 전부를 DTO로 매핑해 반환한다")
        void withItems() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            given(repository.findAll()).willReturn(List.of(
                    postWithId(id1, "t1", "c1"),
                    postWithId(id2, "t2", "c2")
            ));

            List<PostResponseDto> result = postService.getAllPosts();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(PostResponseDto::getId).containsExactly(id1, id2);
            assertThat(result).extracting(PostResponseDto::getTitle).containsExactly("t1", "t2");
            assertThat(result).extracting(PostResponseDto::getContent).containsExactly("c1", "c2");
        }
    }

    @Nested
    @DisplayName("getPost — 단건 조회")
    class GetPost {

        @Test
        @DisplayName("존재하면 해당 글의 DTO를 반환한다")
        void found() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(postWithId(id, "t", "c")));

            PostResponseDto result = postService.getPost(id);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getTitle()).isEqualTo("t");
            assertThat(result.getContent()).isEqualTo("c");
        }

        @Test
        @DisplayName("존재하지 않으면 PostNotFoundException 을 던진다")
        void notFound() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPost(id))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    @DisplayName("update — 수정")
    class Update {

        @Test
        @DisplayName("조회한 엔티티의 update(...)를 호출해 필드를 갱신하고, 갱신된 값을 DTO로 반환한다 (dirty checking 전제)")
        void success() {
            UUID id = UUID.randomUUID();
            Post existing = postWithId(id, "oldT", "oldC");
            given(repository.findById(id)).willReturn(Optional.of(existing));

            PostResponseDto result = postService.update(id,
                    PostRequestDto.builder().title("newT").content("newC").build());

            // 엔티티 상태가 실제로 바뀌었는지 = Post.update 가 호출됐는지의 관찰 가능한 증거
            assertThat(existing.getTitle()).isEqualTo("newT");
            assertThat(existing.getContent()).isEqualTo("newC");
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getTitle()).isEqualTo("newT");
            assertThat(result.getContent()).isEqualTo("newC");

            // 명시적 save 호출은 없어야 한다 (dirty checking 으로 처리되어야 함)
            verify(repository, never()).save(any(Post.class));
        }

        @Test
        @DisplayName("존재하지 않으면 PostNotFoundException 을 던진다")
        void notFound() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postService.update(id,
                    PostRequestDto.builder().title("t").content("c").build()))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining(id.toString());

            verify(repository, never()).save(any(Post.class));
        }
    }

    @Nested
    @DisplayName("delete — 삭제")
    class Delete {

        @Test
        @DisplayName("존재하면 deleteById 를 호출한다")
        void success() {
            UUID id = UUID.randomUUID();
            given(repository.existsById(id)).willReturn(true);

            postService.delete(id);

            verify(repository).deleteById(id);
        }

        @Test
        @DisplayName("존재하지 않으면 PostNotFoundException 을 던지고 deleteById 는 호출하지 않는다")
        void notFound() {
            UUID id = UUID.randomUUID();
            given(repository.existsById(id)).willReturn(false);

            assertThatThrownBy(() -> postService.delete(id))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining(id.toString());

            verify(repository, never()).deleteById(any(UUID.class));
        }
    }
}
