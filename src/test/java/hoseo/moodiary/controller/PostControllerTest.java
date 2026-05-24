package hoseo.moodiary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoseo.moodiary.config.SecurityConfig;
import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.exception.PostAccessDeniedException;
import hoseo.moodiary.exception.PostNotFoundException;
import hoseo.moodiary.security.JwtTokenProvider;
import hoseo.moodiary.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostController 웹 레이어 단위 테스트.
 *
 * <p>{@code @AuthenticationPrincipal UUID userId}로 현재 사용자를 받는 컨트롤러에 대해
 * {@link #asUser(UUID)} 헬퍼로 UUID 를 principal 로 가지는 Authentication 을 주입한다.
 * 실제 운영에서는 {@code JwtAuthenticationFilter}가 그 역할.
 */
@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    /** principal 이 UUID 인 Authentication 을 SecurityContext 에 주입하는 헬퍼. */
    private static RequestPostProcessor asUser(UUID userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Nested
    @DisplayName("POST /post — 게시글 생성")
    class Create {

        @Test
        @DisplayName("정상 입력이면 201과 새 게시글 UUID 를 반환한다")
        void create_success() throws Exception {
            UUID newId = UUID.randomUUID();
            given(postService.create(eq(USER_ID), any(PostRequestDto.class))).willReturn(newId);

            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("hello").content("first post").build());

            mockMvc.perform(post("/post").with(asUser(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("\"" + newId + "\""));
        }

        @Test
        @DisplayName("title 빈 값이면 400")
        void create_blankTitle_returns400() throws Exception {
            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("").content("body").build());

            mockMvc.perform(post("/post").with(asUser(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("제목은 필수입니다."));

            verify(postService, never()).create(any(), any());
        }

        @Test
        @DisplayName("content 빈 값이면 400")
        void create_blankContent_returns400() throws Exception {
            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("title").content("").build());

            mockMvc.perform(post("/post").with(asUser(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("내용은 필수입니다."));
        }

        @Test
        @DisplayName("JSON 파싱 실패면 400과 일반 메시지")
        void create_malformedJson_returns400() throws Exception {
            mockMvc.perform(post("/post").with(asUser(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not-json}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."));
        }
    }

    @Nested
    @DisplayName("GET /post — 내 게시글 전체 조회")
    class FindAll {

        @Test
        @DisplayName("본인 글이 없으면 200과 빈 배열")
        void empty() throws Exception {
            given(postService.getAllPosts(USER_ID)).willReturn(List.of());

            mockMvc.perform(get("/post").with(asUser(USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("본인 글이 여러 개면 200과 항목 전부")
        void withItems() throws Exception {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            given(postService.getAllPosts(USER_ID)).willReturn(List.of(
                    PostResponseDto.builder().id(id1).title("t1").content("c1").build(),
                    PostResponseDto.builder().id(id2).title("t2").content("c2").build()
            ));

            mockMvc.perform(get("/post").with(asUser(USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(id1.toString()))
                    .andExpect(jsonPath("$[1].title").value("t2"));
        }
    }

    @Nested
    @DisplayName("GET /post/{id} — 단건 조회")
    class FindOne {

        @Test
        @DisplayName("본인 글이면 200과 본문 반환")
        void ownedSuccess() throws Exception {
            UUID id = UUID.randomUUID();
            given(postService.getPost(USER_ID, id)).willReturn(
                    PostResponseDto.builder().id(id).title("t").content("c").build());

            mockMvc.perform(get("/post/{id}", id).with(asUser(USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.title").value("t"));
        }

        @Test
        @DisplayName("존재하지 않는 ID면 404")
        void notFound() throws Exception {
            UUID id = UUID.randomUUID();
            given(postService.getPost(USER_ID, id)).willThrow(new PostNotFoundException(id));

            mockMvc.perform(get("/post/{id}", id).with(asUser(USER_ID)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value("게시글을 찾을 수 없습니다. id=" + id));
        }

        @Test
        @DisplayName("타인 글이면 403")
        void notOwned() throws Exception {
            UUID id = UUID.randomUUID();
            given(postService.getPost(USER_ID, id)).willThrow(new PostAccessDeniedException(id));

            mockMvc.perform(get("/post/{id}", id).with(asUser(USER_ID)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value("게시글에 접근할 권한이 없습니다. id=" + id));
        }
    }

    @Nested
    @DisplayName("PUT /post/{id} — 수정")
    class Update {

        @Test
        @DisplayName("본인 글이면 200과 수정된 본문")
        void ownedSuccess() throws Exception {
            UUID id = UUID.randomUUID();
            given(postService.update(eq(USER_ID), eq(id), any(PostRequestDto.class))).willReturn(
                    PostResponseDto.builder().id(id).title("newT").content("newC").build());

            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("newT").content("newC").build());

            mockMvc.perform(put("/post/{id}", id).with(asUser(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("newT"));
        }

        @Test
        @DisplayName("존재하지 않는 ID면 404")
        void notFound() throws Exception {
            UUID id = UUID.randomUUID();
            willThrow(new PostNotFoundException(id))
                    .given(postService).update(eq(USER_ID), eq(id), any(PostRequestDto.class));

            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("t").content("c").build());

            mockMvc.perform(put("/post/{id}", id).with(asUser(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("타인 글이면 403")
        void notOwned() throws Exception {
            UUID id = UUID.randomUUID();
            willThrow(new PostAccessDeniedException(id))
                    .given(postService).update(eq(USER_ID), eq(id), any(PostRequestDto.class));

            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("newT").content("newC").build());

            mockMvc.perform(put("/post/{id}", id).with(asUser(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("body 검증 실패 시 400, 서비스 미호출")
        void validationFails() throws Exception {
            UUID id = UUID.randomUUID();
            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("").content("c").build());

            mockMvc.perform(put("/post/{id}", id).with(asUser(USER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(postService, never()).update(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /post/{id} — 삭제")
    class Delete {

        @Test
        @DisplayName("본인 글이면 204")
        void ownedSuccess() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/post/{id}", id).with(asUser(USER_ID)))
                    .andExpect(status().isNoContent());

            verify(postService).delete(USER_ID, id);
        }

        @Test
        @DisplayName("존재하지 않는 ID면 404")
        void notFound() throws Exception {
            UUID id = UUID.randomUUID();
            willThrow(new PostNotFoundException(id))
                    .given(postService).delete(USER_ID, id);

            mockMvc.perform(delete("/post/{id}", id).with(asUser(USER_ID)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("타인 글이면 403")
        void notOwned() throws Exception {
            UUID id = UUID.randomUUID();
            willThrow(new PostAccessDeniedException(id))
                    .given(postService).delete(USER_ID, id);

            mockMvc.perform(delete("/post/{id}", id).with(asUser(USER_ID)))
                    .andExpect(status().isForbidden());
        }
    }

    /** 미인증 — SecurityFilterChain 잠금이 의도대로 동작하는지 검증. */
    @Nested
    @DisplayName("미인증 접근 — 401")
    class Unauthenticated {

        @Test
        @DisplayName("GET /post 인증 없으면 401")
        void getAll_unauthorized() throws Exception {
            mockMvc.perform(get("/post"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

            verify(postService, never()).getAllPosts(any());
        }

        @Test
        @DisplayName("POST /post 인증 없으면 401")
        void create_unauthorized() throws Exception {
            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("t").content("c").build());

            mockMvc.perform(post("/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());

            verify(postService, never()).create(any(), any());
        }

        @Test
        @DisplayName("DELETE /post/{id} 인증 없으면 401")
        void delete_unauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/post/{id}", id))
                    .andExpect(status().isUnauthorized());

            verify(postService, never()).delete(any(), any());
        }
    }
}
