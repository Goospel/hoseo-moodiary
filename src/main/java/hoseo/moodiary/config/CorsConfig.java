package hoseo.moodiary.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 설정.
 *
 * <p>{@link CorsConfigurationSource} 빈을 제공하면 {@code SecurityConfig} 의
 * {@code .cors(Customizer.withDefaults())} 가 자동으로 이 빈을 찾아 사용한다.
 *
 * <p>허용 origin 은 {@code app.cors.allowed-origins} 설정 키에서 받는다 — 콤마 구분 문자열.
 * 운영에서는 {@code APP_CORS_ALLOWED_ORIGINS} 환경변수로 override (S3 endpoint URL 등 추가).
 *
 * <p><b>왜 origin 패턴 매칭 X 정확한 URL 만</b>: 와일드카드(*) 는 보안상 X.
 * S3 endpoint 패턴(`*.s3-website.*.amazonaws.com`) 도 의도치 않은 다른 S3 버킷을 허용하므로
 * 정확한 URL 만 허용한다.
 *
 * <p><b>credentials</b>: 현재 JWT 는 {@code Authorization} 헤더로 전달 — 쿠키 인증이 아니므로
 * {@code allowCredentials=true} 가 필수는 아니지만, 향후 쿠키 인증 도입을 대비해 true.
 * {@code allowedOrigins} 에 와일드카드 (*) 를 쓰면 {@code allowCredentials=true} 와 충돌해
 * 브라우저가 거절 — 정확한 URL 만 쓰는 정책이 이 둘을 동시에 만족시킨다.
 */
@Configuration
public class CorsConfig {

    /**
     * 콤마 구분 origin 목록. 예: {@code http://localhost:5173,http://moodiary-frontend.s3-website.ap-northeast-2.amazonaws.com}
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        // 허용 헤더 — Authorization (JWT) + Content-Type 이 핵심. 와일드카드 사용도 가능하지만 명시가 안전.
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));
        // 브라우저가 응답에서 직접 읽을 수 있는 헤더 (보통 필요 없음. 향후 페이지네이션 헤더 등 추가 가능)
        config.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        config.setAllowCredentials(true);
        // preflight 캐시 (초). 브라우저가 OPTIONS 를 매번 보내지 않게 함.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
