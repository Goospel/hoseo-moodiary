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
