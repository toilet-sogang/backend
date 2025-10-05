package hwalibo.toilet.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hwalibo.toilet.auth.handler.OAuth2SuccessHandler;
import hwalibo.toilet.auth.jwt.JwtAuthenticationFilter;
import hwalibo.toilet.auth.jwt.JwtTokenProvider;
import hwalibo.toilet.auth.service.CustomOAuth2UserService;
import hwalibo.toilet.dto.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate; // ✅ 추가됨

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CORS 설정 (CorsConfig의 Bean 자동참조)
                .cors(withDefaults())

                // ✅ 기본 인증, CSRF, 폼, 로그아웃 비활성화
                .httpBasic(b -> b.disable())
                .csrf(c -> c.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())

                // ✅ OAuth2 과정 중 세션 잠깐 필요하므로 IF_REQUIRED
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // ✅ 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // Preflight 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 엔드포인트
                        .requestMatchers("/", "/index.html", "/auth/callback.html", "/auth/refresh","/redis/ping").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )

                // ✅ 전역 AuthenticationEntryPoint 등록
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .defaultAuthenticationEntryPointFor(
                                restAuthenticationEntryPoint(),
                                new AntPathRequestMatcher("/auth/**"))
                )

                // ✅ OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                )

                // ✅ JWT 필터 등록 (Redis 블랙리스트 포함)
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // ✅ 전역 401 응답 EntryPoint
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            // CORS 헤더 반드시 추가
            String origin = request.getHeader("Origin");
            if (origin != null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Vary", "Origin");
            }

            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Headers",
                    "Authorization, Content-Type, X-Requested-With, Access-Token, Refresh-Token");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            response.setHeader("Access-Control-Expose-Headers",
                    "Authorization, Access-Token, Refresh-Token");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

            ApiResponse<Object> body = new ApiResponse<>(false, 401, "인증이 필요합니다.");
            new ObjectMapper().writeValue(response.getWriter(), body);
        };
    }
}

