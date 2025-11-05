package hwalibo.toilet.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173", // 로컬 프론트엔드 (Vite)
                        "https://frontendproj-git-main-choiys-projects.vercel.app", // Vercel 배포 주소
                        "https://hwalibo-backend.duckdns.org"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true)
                .maxAge(3600);
    }
}
