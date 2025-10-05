package hwalibo.toilet.config.cors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();

        // 실제 사용하는 프론트 도메인/포트를 정확히 나열 (credentials=true 이므로 * 불가)
        c.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8888",
                "http://127.0.0.1:8888",
                "https://hwalibo.com"
        ));

        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Requested-With",
                "Access-Token", "Refresh-Token" // ← 커스텀 헤더 쓰는 경우 포함
        ));
        c.setExposedHeaders(List.of(
                "Authorization", "Access-Token", "Refresh-Token" // ← 프론트에서 읽게 노출
        ));
        c.setAllowCredentials(true); // 쿠키/인증정보 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}

