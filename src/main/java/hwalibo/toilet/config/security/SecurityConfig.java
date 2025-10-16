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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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
    private final RedisTemplate<String, String> redisTemplate;

    // 전역 401 응답 EntryPoint (필터/컨트롤러에서 전달한 ex.getMessage() 우선 사용)
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            // --- CORS 헤더 설정  ---
            String origin = request.getHeader("Origin");
            if (origin != null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Vary", "Origin");
            }
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-control-allow-headers",
                    "Authorization, Content-Type, X-Requested-With, Access-Token, Refresh-Token");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            response.setHeader("Access-Control-Expose-Headers",
                    "Authorization, Access-Token, Refresh-Token");
            // --- ------------------------------------ ---

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");


            // 1. 기본 에러 메시지를 "로그인이 필요합니다."로 설정
            String errorMessage = "로그인이 필요합니다.";

            // 2. 만약 예외에 메시지가 있고, 그게 우리가 바꾸고 싶은 기본 메시지가 아니라면
            //    (예: "로그아웃된 사용자입니다." 같은 커스텀 메시지라면) 그 메시지를 사용
            if (ex != null && ex.getMessage() != null && !ex.getMessage().equalsIgnoreCase("Full authentication is required to access this resource")) {
                errorMessage = ex.getMessage();
            }

            new ObjectMapper().writeValue(
                    response.getWriter(),
                    new ApiResponse<>(false, 401, errorMessage) // 최종 메시지를 담아 응답
            );
        };
    }

    // ✅ JwtAuthenticationFilter를 Bean으로 등록 (EntryPoint 주입)
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AuthenticationEntryPoint restAuthenticationEntryPoint) {
        return new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate, restAuthenticationEntryPoint);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(withDefaults())

                // 기본 인증/CSRF/폼/로그아웃 비활성화
                .httpBasic(h -> h.disable())
                .csrf(c -> c.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .anonymous(withDefaults())

                //OAuth2 리다이렉트 동안만 세션 사용
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // 요청별 권한
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/index.html", "/auth/callback.html",
                                "/auth/refresh", "/redis/ping").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 전역 EntryPoint (필터/보호자원에서 인증 실패 시 공통 401 JSON)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint()))

                // OAuth2 로그인
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                )

                // JWT 필터(Bean) 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

