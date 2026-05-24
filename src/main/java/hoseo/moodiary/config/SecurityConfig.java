package hoseo.moodiary.config;

import hoseo.moodiary.security.JwtAuthenticationFilter;
import hoseo.moodiary.security.JwtTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 설정.
 *
 * <p><b>PR 2-c (현재 시점)</b> — JWT 검증 필터를 추가.
 * {@code Authorization: Bearer <token>} 헤더가 있으면 {@link JwtAuthenticationFilter}가 검증 후
 * {@code SecurityContext}에 인증을 채운다. 토큰이 없거나 잘못되면 인증 없이 통과 →
 * 보호된 자원이면 401로 거절.
 *
 * <p>화이트리스트({@link #WHITELIST}) 외 요청은 {@code authenticated()}.
 * 미인증 시 401 + {@code {"message":"인증이 필요합니다."}} JSON 반환.
 *
 * <p>{@link JwtProperties}는 {@link EnableConfigurationProperties}로 활성화 — 메인 클래스에 의존성을 두지 않아
 * 슬라이스 테스트와의 호환성을 유지한다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
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
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
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
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
