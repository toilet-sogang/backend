package hwalibo.toilet.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import hwalibo.toilet.auth.jwt.JwtAuthenticationFilter;
import hwalibo.toilet.dto.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import hwalibo.toilet.auth.handler.OAuth2SuccessHandler;
import hwalibo.toilet.auth.jwt.JwtTokenProvider;
import hwalibo.toilet.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize, @Secured 애노테이션을 이용한 메서드 단위 권한 제어 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .httpBasic(b -> b.disable())
                .csrf(c -> c.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // OPTIONS 허용 (CORS preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 엔드포인트
                        .requestMatchers("/", "/index.html", "/auth/refresh", "/auth/callback.html").permitAll()
                        .requestMatchers("/oauth2/**","/login/oauth2/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 그 외 보호
                        .anyRequest().authenticated()
                )

                // 토큰 없음(미인증) 시 특정 API 경로는 401 JSON 반환 (리다이렉트 방지)
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(restAuthenticationEntryPoint(), new AntPathRequestMatcher("/user/**"))
                        .defaultAuthenticationEntryPointFor(restAuthenticationEntryPoint(), new AntPathRequestMatcher("/auth/**"))
                        .defaultAuthenticationEntryPointFor(restAuthenticationEntryPoint(), new AntPathRequestMatcher("/toilet/**"))
                        .defaultAuthenticationEntryPointFor(restAuthenticationEntryPoint(), new AntPathRequestMatcher("/station/**"))
                )

                // OAuth2 로그인
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                )

                // JWT 필터
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // SecurityConfig 내부에 추가
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            // CORS 헤더 (브라우저가 401 응답을 읽을 수 있도록)
            String origin = request.getHeader("Origin");
            if (origin != null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Vary", "Origin");
            }
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            response.setHeader("Access-Control-Expose-Headers", "Authorization");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

            ApiResponse<Object> body = new ApiResponse<>(false, 401, "인증이 필요합니다.");
            new ObjectMapper().writeValue(response.getWriter(), body);
        };
    }
}
