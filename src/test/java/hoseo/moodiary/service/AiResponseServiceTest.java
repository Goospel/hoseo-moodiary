package hoseo.moodiary.service;

import hoseo.moodiary.dto.response.AiResponseDto;
import hoseo.moodiary.entitiy.AiResponse;
import hoseo.moodiary.entitiy.AiResponseStatus;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.AiInferenceException;
import hoseo.moodiary.exception.AiResponseNotFoundException;
import hoseo.moodiary.exception.PostAccessDeniedException;
import hoseo.moodiary.repository.AiResponseJpaRepository;
import hoseo.moodiary.repository.PostJpaRepository;
import hoseo.moodiary.service.ai.AiInferenceResult;
import hoseo.moodiary.service.ai.AiResponseClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/**
 * AiResponseService 단위 테스트.
 *
 * <p>외부 호출 ({@link AiResponseClient}) 은 mock. {@code @Async} 어노테이션의 비동기 디스패치는
 * 단위 테스트에서 동기 실행됨 (proxy 미적용) — 호출 흐름 검증에는 충분.
 */
@ExtendWith(MockitoExtension.class)
class AiResponseServiceTest {

    @Mock
    private AiResponseJpaRepository aiResponseRepository;

    @Mock
    private PostJpaRepository postRepository;

    @Mock
    private AiResponseClient client;

    @InjectMocks
    private AiResponseService aiResponseService;

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static User userWithId(UUID id) {
        User user = User.builder().email("a@b.com").password("HASHED").nickname("nick").build();
        setField(user, "id", id);
        return user;
    }

    private static Post postWithId(UUID postId, UUID ownerId, String title, String content) {
        Post post = Post.builder().title(title).content(content).user(userWithId(ownerId)).build();
        setField(post, "id", postId);
        return post;
    }

    private static AiResponse pendingFor(Post post) {
        return AiResponse.builder().post(post).build();
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    @DisplayName("triggerAsync — 비동기 추론")
    class TriggerAsync {

        @Test
        @DisplayName("어댑터 성공 시 PENDING → DONE 으로 전이하고 content/emoji 를 채운다")
        void success_transitionsToDone() {
            UUID postId = UUID.randomUUID();
            Post post = postWithId(postId, OWNER_ID, "title", "content");
            AiResponse pending = pendingFor(post);
            given(aiResponseRepository.findByPost_Id(postId)).willReturn(Optional.of(pending));
            given(client.invoke(postId, "title", "content"))
                    .willReturn(new AiInferenceResult("AI 응답 본문", "😊"));

            aiResponseService.triggerAsync(postId);

            assertThat(pending.getStatus()).isEqualTo(AiResponseStatus.DONE);
            assertThat(pending.getContent()).isEqualTo("AI 응답 본문");
            assertThat(pending.getEmoji()).isEqualTo("😊");
            assertThat(pending.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("어댑터가 AiInferenceException 을 던지면 PENDING → FAILED 로 전이하고 errorMessage 를 저장한다")
        void inferenceFails_transitionsToFailed() {
            UUID postId = UUID.randomUUID();
            Post post = postWithId(postId, OWNER_ID, "title", "content");
            AiResponse pending = pendingFor(post);
            given(aiResponseRepository.findByPost_Id(postId)).willReturn(Optional.of(pending));
            willThrow(new AiInferenceException("AI 서버 응답 시간 초과"))
                    .given(client).invoke(postId, "title", "content");

            aiResponseService.triggerAsync(postId);

            assertThat(pending.getStatus()).isEqualTo(AiResponseStatus.FAILED);
            assertThat(pending.getErrorMessage()).isEqualTo("AI 서버 응답 시간 초과");
            assertThat(pending.getContent()).isNull();
            assertThat(pending.getEmoji()).isNull();
        }

        @Test
        @DisplayName("PENDING row 가 없으면 AiResponseNotFoundException (이론상 발생 X — PENDING 은 일기 저장과 같은 트랜잭션에 생성됨)")
        void pendingMissing_throws() {
            UUID postId = UUID.randomUUID();
            given(aiResponseRepository.findByPost_Id(postId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiResponseService.triggerAsync(postId))
                    .isInstanceOf(AiResponseNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getByPostId — 폴링 조회")
    class GetByPostId {

        @Test
        @DisplayName("본인 글의 DONE 상태면 모든 필드를 채운 DTO 반환")
        void ownedDone_returnsFullDto() {
            UUID postId = UUID.randomUUID();
            Post post = postWithId(postId, OWNER_ID, "title", "content");
            AiResponse done = pendingFor(post);
            done.markDone("AI 본문", "😊");
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(aiResponseRepository.findByPost_Id(postId)).willReturn(Optional.of(done));

            AiResponseDto result = aiResponseService.getByPostId(OWNER_ID, postId);

            assertThat(result.getPostId()).isEqualTo(postId);
            assertThat(result.getStatus()).isEqualTo(AiResponseStatus.DONE);
            assertThat(result.getContent()).isEqualTo("AI 본문");
            assertThat(result.getEmoji()).isEqualTo("😊");
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("본인 글의 FAILED 상태면 errorMessage 를 채워서 반환")
        void ownedFailed_includesErrorMessage() {
            UUID postId = UUID.randomUUID();
            Post post = postWithId(postId, OWNER_ID, "title", "content");
            AiResponse failed = pendingFor(post);
            failed.markFailed("AI 서버 응답 시간 초과");
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(aiResponseRepository.findByPost_Id(postId)).willReturn(Optional.of(failed));

            AiResponseDto result = aiResponseService.getByPostId(OWNER_ID, postId);

            assertThat(result.getStatus()).isEqualTo(AiResponseStatus.FAILED);
            assertThat(result.getErrorMessage()).isEqualTo("AI 서버 응답 시간 초과");
            assertThat(result.getContent()).isNull();
            assertThat(result.getEmoji()).isNull();
        }

        @Test
        @DisplayName("Post 가 없으면 AiResponseNotFoundException — postId 노출 정책에 따라 404 통일")
        void postMissing_throws() {
            UUID postId = UUID.randomUUID();
            given(postRepository.findById(postId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiResponseService.getByPostId(OWNER_ID, postId))
                    .isInstanceOf(AiResponseNotFoundException.class);
        }

        @Test
        @DisplayName("타인 글이면 PostAccessDeniedException — Post 존재 노출 방지를 위해 lookup 직후 권한 검증")
        void notOwned_throws() {
            UUID postId = UUID.randomUUID();
            Post post = postWithId(postId, OTHER_USER_ID, "title", "content");
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            assertThatThrownBy(() -> aiResponseService.getByPostId(OWNER_ID, postId))
                    .isInstanceOf(PostAccessDeniedException.class);
        }

        @Test
        @DisplayName("Post 는 있는데 AiResponse 가 없으면 AiResponseNotFoundException (이론상 발생 X — 일기 저장 시 PENDING 도 동시 생성)")
        void aiResponseMissing_throws() {
            UUID postId = UUID.randomUUID();
            Post post = postWithId(postId, OWNER_ID, "title", "content");
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(aiResponseRepository.findByPost_Id(postId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> aiResponseService.getByPostId(OWNER_ID, postId))
                    .isInstanceOf(AiResponseNotFoundException.class);
        }
    }
}
