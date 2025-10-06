package hwalibo.toilet.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import hwalibo.toilet.dto.global.response.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static hwalibo.toilet.auth.jwt.JwtConstants.HEADER_STRING;
import static hwalibo.toilet.auth.jwt.JwtConstants.TOKEN_PREFIX;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthenticationEntryPoint entryPoint; // 전역 EntryPoint 주입

    private static final List<AntPathRequestMatcher> SKIP_MATCHERS = List.of(
            new AntPathRequestMatcher("/auth/refresh"),
            new AntPathRequestMatcher("/auth/callback.html"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/redis/ping"),
            new AntPathRequestMatcher("/")
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        return SKIP_MATCHERS.stream().anyMatch(m -> m.matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 블랙리스트(로그아웃 토큰)
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                entryPoint.commence(request, response,
                        new org.springframework.security.core.AuthenticationException("로그아웃된 사용자입니다.") {});
                return;
            }

            // 유효성 검사
            if (jwtTokenProvider.validateToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response,
                        new org.springframework.security.core.AuthenticationException("유효하지 않은 토큰입니다.") {});
                return;
            }

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response,
                    new org.springframework.security.core.AuthenticationException("JWT 오류: " + e.getMessage()) {});
            return;
        }

        chain.doFilter(request, response);
    }

    /** Authorization: Bearer <token> 에서 토큰만 추출 */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER_STRING);       // ← static import 사용
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_PREFIX)) {
            return bearer.substring(TOKEN_PREFIX.length());     // ← static import 사용
        }
        return null;
    }
}