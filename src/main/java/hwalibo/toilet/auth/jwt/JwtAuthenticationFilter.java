package hwalibo.toilet.auth.jwt;

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

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthenticationEntryPoint entryPoint;

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
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("OPTIONS 요청은 필터를 통과합니다.");
            return true;
        }
        boolean skip = SKIP_MATCHERS.stream().anyMatch(m -> m.matches(request));
        if (skip) {
            log.info("요청 URL이 필터 제외 목록에 포함되어 필터를 통과합니다: {}", request.getRequestURI());
        }
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        log.info("토큰 추출 시도: {}", token);

        if (!StringUtils.hasText(token)) {
            log.warn("토큰이 없거나 비어 있음. 필터를 계속 진행합니다.");
            chain.doFilter(request, response);
            return;
        }

        try {
            // 블랙리스트(로그아웃 토큰) 체크
            log.info("블랙리스트 확인 중: {}", token);
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                log.warn("로그아웃된 사용자입니다. 블랙리스트에 토큰이 존재: {}", token);
                entryPoint.commence(request, response,
                        new org.springframework.security.core.AuthenticationException("로그아웃된 사용자입니다.") {});
                return;
            }

            // 유효성 검사
            log.info("토큰 유효성 검사 중: {}", token);
            if (jwtTokenProvider.validateToken(token)) {
                log.info("유효한 토큰입니다. 인증 정보를 SecurityContext에 설정합니다.");
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("유효하지 않은 토큰입니다: {}", token);
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response,
                        new org.springframework.security.core.AuthenticationException("유효하지 않은 토큰입니다.") {});
                return;
            }

        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response,
                    new org.springframework.security.core.AuthenticationException("JWT 오류: " + e.getMessage()) {});
            return;
        }
        chain.doFilter(request, response);
    }

    // Authorization: Bearer <token>에서 토큰만 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER_STRING);
        if (StringUtils.hasText(bearer) && bearer.startsWith(TOKEN_PREFIX)) {
            log.info("Authorization 헤더에서 Bearer 토큰 추출 성공: {}", bearer);
            return bearer.substring(TOKEN_PREFIX.length());
        }
        log.warn("Authorization 헤더에서 Bearer 토큰을 찾지 못함.");
        return null;
    }
}
