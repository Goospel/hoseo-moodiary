package hoseo.moodiary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hoseo.moodiary.config.SecurityConfig;
import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.exception.PostNotFoundException;
import hoseo.moodiary.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
 * <p>{@link WebMvcTest}로 PostController 한 개만 로드하고 PostService는 Mockito로 가짜로 주입한다.
 * DB·JPA·실 서비스 로직은 일절 부팅되지 않으므로 빠르고(<1s), 컨트롤러의 라우팅·요청 파싱·검증·
 * 응답 직렬화·예외 매핑(GlobalExceptionHandler)만 격리 검증한다.
 *
 * <p><b>인증 적용 방식</b> — Post API는 모두 {@code authenticated()} 잠금 상태이므로 각 요청에
 * {@code .with(user("test"))}로 인증 컨텍스트를 주입한다. {@code @WithMockUser} 어노테이션을
 * 쓰지 않는 이유는 JUnit 5의 {@code @Nested}가 별도 테스트 클래스로 취급되어 outer의 어노테이션이
 * 자동 적용되지 않는 호환성 이슈를 피하기 위함. 요청 레벨 처리가 더 명시적이기도 하다.
 */
@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Spring Boot 4.x의 @WebMvcTest가 Jackson auto-config을 자동 등록하지 않아 직접 생성한다. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PostService postService;

    @Nested
    @DisplayName("POST /post — 게시글 생성")
    class Create {

        @Test
        @DisplayName("정상 입력이면 201과 생성된 게시글 ID(UUID)를 반환한다")
        void create_success() throws Exception {
            UUID newId = UUID.randomUUID();
            given(postService.create(any(PostRequestDto.class))).willReturn(newId);

            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("hello").content("first post").build());

            mockMvc.perform(post("/post").with(user("test"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("\"" + newId + "\""));
        }

        @Test
        @DisplayName("title이 비어있으면 400과 검증 메시지를 반환한다")
        void create_blankTitle_returns400() throws Exception {
            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("").content("body").build());

            mockMvc.perform(post("/post").with(user("test"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("제목은 필수입니다."));

            verify(postService, never()).create(any());
        }

        @Test
        @DisplayName("content가 비어있으면 400과 검증 메시지를 반환한다")
        void create_blankContent_returns400() throws Exception {
            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("title").content("").build());

            mockMvc.perform(post("/post").with(user("test"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("내용은 필수입니다."));
        }

        @Test
        @DisplayName("JSON 파싱이 실패하면 400과 일반 메시지를 반환한다")
        void create_malformedJson_returns400() throws Exception {
            mockMvc.perform(post("/post").with(user("test"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not-json}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."));
        }
    }

    @Nested
    @DisplayName("GET /post — 전체 조회")
    class FindAll {

        @Test
        @DisplayName("게시글이 없으면 200과 빈 배열을 반환한다")
        void findAll_empty() throws Exception {
            given(postService.getAllPosts()).willReturn(List.of());

            mockMvc.perform(get("/post").with(user("test")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("게시글이 여러 개이면 200과 모든 항목을 반환한다")
        void findAll_withItems() throws Exception {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            given(postService.getAllPosts()).willReturn(List.of(
                    PostResponseDto.builder().id(id1).title("t1").content("c1").build(),
                    PostResponseDto.builder().id(id2).title("t2").content("c2").build()
            ));

            mockMvc.perform(get("/post").with(user("test")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(id1.toString()))
                    .andExpect(jsonPath("$[0].title").value("t1"))
                    .andExpect(jsonPath("$[1].content").value("c2"));
        }
    }

    @Nested
    @DisplayName("GET /post/{id} — 단건 조회")
    class FindOne {

        @Test
        @DisplayName("존재하는 ID면 200과 게시글 본문을 반환한다")
        void findOne_success() throws Exception {
            UUID id = UUID.randomUUID();
            given(postService.getPost(id)).willReturn(
                    PostResponseDto.builder().id(id).title("t").content("c").build());

            mockMvc.perform(get("/post/{id}", id).with(user("test")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.title").value("t"))
                    .andExpect(jsonPath("$.content").value("c"));
        }

        @Test
        @DisplayName("존재하지 않는 ID면 404와 도메인 메시지를 반환한다")
        void findOne_notFound() throws Exception {
            UUID id = UUID.randomUUID();
            given(postService.getPost(id)).willThrow(new PostNotFoundException(id));

            mockMvc.perform(get("/post/{id}", id).with(user("test")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value("게시글을 찾을 수 없습니다. id=" + id));
        }
    }

    @Nested
    @DisplayName("PUT /post/{id} — 수정")
    class Update {

        @Test
        @DisplayName("정상 입력이면 200과 수정된 본문을 반환한다")
        void update_success() throws Exception {
            UUID id = UUID.randomUUID();
            given(postService.update(eq(id), any(PostRequestDto.class))).willReturn(
                    PostResponseDto.builder().id(id).title("newT").content("newC").build());

            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("newT").content("newC").build());

            mockMvc.perform(put("/post/{id}", id).with(user("test"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("newT"))
                    .andExpect(jsonPath("$.content").value("newC"));
        }

        @Test
        @DisplayName("존재하지 않는 ID면 404를 반환한다")
        void update_notFound() throws Exception {
            UUID id = UUID.randomUUID();
            willThrow(new PostNotFoundException(id))
                    .given(postService).update(eq(id), any(PostRequestDto.class));

            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("t").content("c").build());

            mockMvc.perform(put("/post/{id}", id).with(user("test"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value("게시글을 찾을 수 없습니다. id=" + id));
        }

        @Test
        @DisplayName("body가 잘못되면 400을 반환하고 서비스는 호출되지 않는다")
        void update_validationFails() throws Exception {
            UUID id = UUID.randomUUID();
            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("").content("c").build());

            mockMvc.perform(put("/post/{id}", id).with(user("test"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(postService, never()).update(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /post/{id} — 삭제")
    class Delete {

        @Test
        @DisplayName("존재하는 ID면 204 No Content를 반환한다")
        void delete_success() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/post/{id}", id).with(user("test")))
                    .andExpect(status().isNoContent());

            verify(postService).delete(id);
        }

        @Test
        @DisplayName("존재하지 않는 ID면 404를 반환한다")
        void delete_notFound() throws Exception {
            UUID id = UUID.randomUUID();
            willThrow(new PostNotFoundException(id))
                    .given(postService).delete(id);

            mockMvc.perform(delete("/post/{id}", id).with(user("test")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value("게시글을 찾을 수 없습니다. id=" + id));
        }
    }

    /**
     * SecurityFilterChain 잠금이 의도대로 동작하는지 검증.
     * Post API 는 모두 {@code authenticated()} 상태여야 한다.
     * 이 블록은 인증을 의도적으로 누락한다.
     */
    @Nested
    @DisplayName("미인증 접근 — 401 Unauthorized")
    class Unauthenticated {

        @Test
        @DisplayName("GET /post 는 인증 없으면 401과 표준 에러 메시지를 반환하고 서비스에 도달하지 않는다")
        void getAll_unauthorized() throws Exception {
            mockMvc.perform(get("/post"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

            verify(postService, never()).getAllPosts();
        }

        @Test
        @DisplayName("POST /post 는 인증 없으면 401")
        void create_unauthorized() throws Exception {
            String body = objectMapper.writeValueAsString(
                    PostRequestDto.builder().title("t").content("c").build());

            mockMvc.perform(post("/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());

            verify(postService, never()).create(any());
        }

        @Test
        @DisplayName("DELETE /post/{id} 는 인증 없으면 401")
        void delete_unauthorized() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/post/{id}", id))
                    .andExpect(status().isUnauthorized());

            verify(postService, never()).delete(any());
        }
    }
}
