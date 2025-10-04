package hwalibo.toilet.config.cors;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // ✅ 시큐리티 필터보다 먼저
public class GlobalCorsFilter implements Filter {

    // 필요한 출처만!
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:8080",
            "http://127.0.0.1:8080",
            "http://localhost:3000",
            "https://hwalibo.com"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String method  = request.getMethod();
        String uri     = request.getRequestURI();
        String origin  = request.getHeader("Origin");
        String acrm    = request.getHeader("Access-Control-Request-Method");
        String acrh    = request.getHeader("Access-Control-Request-Headers");

        log.debug("CORS ⇢ method={}, uri={}, Origin={}, ACRM={}, ACRH={}",
                method, uri, origin, acrm, acrh);

        // 공통 헤더
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin"); // 캐시 분리
        } else if (origin != null) {
            log.debug("CORS ✗ Origin not allowed: {}", origin);
        }
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
        response.setHeader("Access-Control-Expose-Headers", "Authorization");

        // 프리플라이트는 여기서 종료
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("CORS ✓ Preflight OK → 200 (uri={})", uri);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);

        // 응답 로그
        log.debug("CORS ⇠ method={}, uri={}, status={}, ACAO={}",
                method, uri, response.getStatus(),
                response.getHeader("Access-Control-Allow-Origin"));
    }
}
