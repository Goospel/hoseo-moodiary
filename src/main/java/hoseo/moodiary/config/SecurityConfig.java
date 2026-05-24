package hoseo.moodiary.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 설정.
 *
 * <p><b>PR 2-b (현재 시점)</b> — 화이트리스트 외 모든 엔드포인트에 인증을 요구한다.
 * <ul>
 *   <li>{@link #WHITELIST} 외 요청은 {@code authenticated()} — 미인증이면 401 JSON 반환.</li>
 *   <li>세션은 {@link SessionCreationPolicy#STATELESS} — REST API, JWT 도입(PR 2-c) 대비.</li>
 *   <li>CSRF / formLogin / httpBasic / logout 모두 비활성.</li>
 *   <li>{@link PasswordEncoder} 빈: BCrypt.</li>
 * </ul>
 *
 * <p><b>주의</b> — 이 PR 머지 후에는 Post API가 401이 된다. JWT 발급(PR 2-c)이 끝나야 실제 사용자가 호출 가능.
 * 그래서 PR 2-b/2-c/3은 dev에 누적했다가 main에 한 번에 머지하는 흐름.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 인증 없이 접근 가능한 경로.
     * <ul>
     *   <li>{@code /auth/**} — 회원가입/로그인은 인증 자체가 불가능</li>
     *   <li>Swagger/OpenAPI 관련 — 개발 편의 + FE TypeScript 생성을 위해 항상 열어둠</li>
     *   <li>{@code /error} — 스프링 기본 에러 디스패치. 막으면 예외 페이지도 401이 되어 디버깅 불가</li>
     * </ul>
     */
    private static final String[] WHITELIST = {
            "/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/error"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    res.getWriter().write("{\"message\":\"인증이 필요합니다.\"}");
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
