package hoseo.moodiary.config;

import hoseo.moodiary.controller.PostController;
import hoseo.moodiary.security.JwtTokenProvider;
import hoseo.moodiary.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS 통합 검증.
 *
 * <p>임의 컨트롤러 슬라이스(PostController)에 SecurityConfig + CorsConfig 를 끌어와
 * Spring Security 필터 체인 위에서 preflight 가 실제로 어떻게 동작하는지 검증한다.
 *
 * <p>preflight (OPTIONS) 핵심 규칙:
 * <ul>
 *   <li>허용 origin → 200 + {@code Access-Control-Allow-Origin: <그 origin>}</li>
 *   <li>미허용 origin → 403</li>
 *   <li>OPTIONS 자체는 인증 검사 안 거침 — 인증 헤더 없어도 통과해야 정상 동작</li>
 * </ul>
 */
@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://allowed-origin.com,http://localhost:5173"
})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("preflight: 허용 origin → 200 + Access-Control-Allow-Origin 헤더에 그 origin 반환")
    void preflight_allowedOrigin_returns200WithCorsHeader() throws Exception {
        mockMvc.perform(options("/post")
                        .header("Origin", "http://allowed-origin.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://allowed-origin.com"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("preflight: 로컬 dev origin (localhost:5173) 도 허용")
    void preflight_localhostOrigin_returns200() throws Exception {
        mockMvc.perform(options("/post")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("preflight: 미허용 origin → 403 (Spring Security 가 차단)")
    void preflight_disallowedOrigin_returns403() throws Exception {
        mockMvc.perform(options("/post")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("preflight: Authorization 헤더가 Allowed-Headers 에 포함됨 (JWT 호출 필수)")
    void preflight_authorizationHeaderAllowed() throws Exception {
        mockMvc.perform(options("/post")
                        .header("Origin", "http://allowed-origin.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsString("Authorization")));
    }
}
