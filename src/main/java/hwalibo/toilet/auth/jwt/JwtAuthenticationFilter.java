package hwalibo.toilet.auth.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static hwalibo.toilet.auth.jwt.JwtConstants.HEADER_STRING;
import static hwalibo.toilet.auth.jwt.JwtConstants.TOKEN_PREFIX;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 0) 프리플라이트는 무조건 통과 (CORS는 Security의 cors()가 처리)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        final String uri = request.getRequestURI();
        final String method = request.getMethod();

        // 1) Authorization에서 토큰 추출
        String token = resolveToken(request);

        if (!StringUtils.hasText(token)) {
            // 토큰 없음: 조용히 통과 → 인증 필요한 경로면 EntryPoint가 401 생성 + CORS 헤더 부착
            log.debug("JWT skip(no bearer): {} {}", method, uri);
            chain.doFilter(request, response);
            return;
        }

        try {
            // 2) 토큰 유효성 검사
            if (jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT ok: {} {} as {}", method, uri, authentication.getName());
            } else {
                // 유효X: 컨텍스트 비우고 통과 (여기서 401 쓰지 않음)
                SecurityContextHolder.clearContext();
                log.debug("JWT invalid: {} {}", method, uri);
            }
        } catch (JwtException | IllegalArgumentException e) {
            // 파싱/서명/만료 등 문제: 컨텍스트 비우고 통과
            SecurityContextHolder.clearContext();
            log.debug("JWT exception({}): {} {}", e.getClass().getSimpleName(), method, uri);
        }

        chain.doFilter(request, response);
    }

    /** Authorization: Bearer <token> 형태에서 토큰만 추출 */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER_STRING);
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_PREFIX)) {
            return bearer.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}




