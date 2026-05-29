package hoseo.moodiary.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Authorization 헤더에서 Bearer 토큰을 꺼내 검증한 뒤 {@code SecurityContext}에 인증 객체를 채워 넣는다.
 *
 * <p>토큰이 없거나 검증 실패 시에는 그냥 통과시킨다(인증 미설정 상태로 다음 필터에 전달).
 * 보호된 엔드포인트라면 뒤에 있는 {@code AuthorizationFilter}가 401을 띄울 것이고,
 * 화이트리스트(예: {@code /auth/**})라면 그대로 진행되어야 하므로 여기서 즉시 401을 쏘면 안 된다.
 *
 * <p>이 필터는 {@code SecurityConfig}에서 {@code UsernamePasswordAuthenticationFilter} 앞에 등록된다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                UUID userId = jwtTokenProvider.getUserId(token);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                // 검증 실패는 무시하고 통과 — 인증 없는 상태로 다음 필터에 전달.
                // 보호된 자원이면 AuthorizationFilter가 401로 거절한다.
                //
                // T-033 의도적 silent — 이 catch 는 모든 인증 요청을 거치는 high-volume 경로다.
                // 만료 / 위조 / 잘못된 형식 토큰 셋 다 "예상되는 클라이언트 실수" 라 log 떨어뜨리면
                // 분당 수백 라인 floods 가능 (특히 만료된 토큰을 가진 SPA 가 refresh 직전 다수 호출).
                // T-032 의 "silent 500 = 진단 불가" 와는 카테고리가 다른 의도적 침묵.
                // 만약 운영에서 "JWT 인증 실패율" 모니터링이 필요해지면 별도 metrics counter 로 추가
                // (로그가 아닌 메트릭이 적절한 채널 — Micrometer + Prometheus).
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
