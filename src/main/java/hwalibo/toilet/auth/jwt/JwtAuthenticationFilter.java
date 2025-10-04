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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static hwalibo.toilet.auth.jwt.JwtConstants.*;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String authHeader = request.getHeader(HEADER_STRING);

        log.debug("JWT ⇢ {} {} (Authorization present: {})", method, uri, authHeader != null);

        String token = resolveToken(request);

        if (token != null) {
            log.debug("JWT ⇢ token(head.10)= {}...", token.substring(0, Math.min(10, token.length())));
            try {
                // 1) 유효성(서명/만료) 검사
                if (jwtTokenProvider.validateToken(token)) {
                    // 2) 인증 객체 생성 및 컨텍스트 저장
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT ✓ authenticated as {}", authentication.getName());
                } else {
                    // 유효하지 않음 → 즉시 401 JSON 반환
                    SecurityContextHolder.clearContext();
                    addCorsHeaders(request, response);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");

                    ObjectMapper om = new ObjectMapper();
                    ApiResponse<Object> body = new ApiResponse<>(false, 401, "Unauthorized: invalid or expired token");
                    response.getWriter().write(om.writeValueAsString(body));
                    response.getWriter().flush();
                    return;
                }
            } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
                // 파싱 실패/유저 미존재/기타 토큰 오류 → 401 JSON
                SecurityContextHolder.clearContext();
                log.debug("JWT ✗ Rejecting token: {} → 401", ex.getMessage());

                addCorsHeaders(request, response);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");

                ObjectMapper om = new ObjectMapper();
                ApiResponse<Object> body = new ApiResponse<>(false, 401, "Unauthorized: " + ex.getMessage());
                response.getWriter().write(om.writeValueAsString(body));
                response.getWriter().flush();
                return;
            }
        } else {
            // 토큰이 아예 없는 경우: 기본 동작(익명 → 필요시 OAuth2 로그인 302 리다이렉트)에 맡김
            log.debug("JWT ⇢ no Bearer token");
        }

        filterChain.doFilter(request, response);
        log.debug("JWT ⇠ {} {} → status={}", method, uri, response.getStatus());
    }

    // Authorization: Bearer <token> 에서 token만 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_STRING);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    // 401을 브라우저가 읽을 수 있도록 CORS 헤더 보강
    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        }
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
    }
}




