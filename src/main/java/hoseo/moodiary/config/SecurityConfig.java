package hoseo.moodiary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정.
 *
 * <p><b>PR 2-a (현재 시점)</b> — 회원가입(BCrypt) 도입을 위한 최소 설정.
 * <ul>
 *   <li>{@link PasswordEncoder} 빈만 제공 (BCrypt).</li>
 *   <li>SecurityFilterChain은 의도적으로 <b>permitAll</b> — 인증 도입 전이라 기존 Post API 동작을 깨지 않기 위함.</li>
 *   <li>CSRF / formLogin / httpBasic 모두 비활성 — REST API 전용.</li>
 * </ul>
 *
 * <p><b>PR 2-b 예정</b> — 화이트리스트 외 모든 엔드포인트 {@code authenticated()} 강제. 그때 이 파일을 수정한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
