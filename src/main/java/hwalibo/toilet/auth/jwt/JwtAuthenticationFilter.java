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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static hwalibo.toilet.auth.jwt.JwtConstants.HEADER_STRING;
import static hwalibo.toilet.auth.jwt.JwtConstants.TOKEN_PREFIX;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 1. 로그아웃된 토큰(블랙리스트) 확인
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                log.warn("JWT ✗ blacklisted token → 401");
                unauthorized(response, "로그아웃된 토큰입니다.");
                return;
            }

            // 2. 토큰 유효성 검사
            if (jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT ✓ authenticated as {}", authentication.getName());
            } else {
                SecurityContextHolder.clearContext();
                unauthorized(response, "유효하지 않은 토큰입니다.");
                return;
            }

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            unauthorized(response, "JWT 오류: " + e.getMessage());
            return;
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

    /** 공통 401 응답 메서드 */
    private void unauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        new ObjectMapper().writeValue(response.getWriter(),
                new ApiResponse<>(false, 401, msg));
    }
}




