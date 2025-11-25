package hwalibo.toilet.auth.jwt;

import hwalibo.toilet.auth.CustomOAuth2User;
import hwalibo.toilet.domain.type.Gender;
import hwalibo.toilet.domain.type.Role;
import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.exception.auth.InvalidTokenException;
import hwalibo.toilet.respository.user.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;
    private final UserRepository userRepository;

    private static final String TOKEN_PREFIX = "Bearer ";

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidity,
            UserRepository userRepository) {

        this.key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
        this.accessTokenValidityInMilliseconds = accessTokenValidity * 1000;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidity * 1000;
        this.userRepository = userRepository;
        log.info("JwtTokenProvider initialized with secret key and token validity settings.");
    }

    // Access Token 생성
    public String createAccessToken(Authentication authentication) {

        User user = extractUserFromAuthentication(authentication);
        String authorities = extractAuthorities(authentication);
        Date validity = calculateTokenValidity(this.accessTokenValidityInMilliseconds);

        return buildToken(user.getId().toString(), user.getUsername(), authorities, validity, user.getGender());
    }

    // Refresh Token 생성
    public String createRefreshToken() {
        log.info("Creating refresh token");
        Date validity = calculateTokenValidity(this.refreshTokenValidityInMilliseconds);
        return buildToken(null, null, null, validity, null);
    }

    // Authentication 객체 가져오기 (Stateless)
    public Authentication getAuthentication(String token) {
        log.info("Parsing JWT token to get authentication without DB lookup");
        token = stripBearerPrefix(token);
        Claims claims = parseClaimsFromToken(token);

        // 'auth' 클레임이 없으면 Access Token이 아니므로 예외 발생
        if (claims.get("auth") == null) {
            throw new InvalidTokenException("권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        String genderStr = claims.get("gender", String.class);
        Gender gender = (genderStr != null) ? Gender.valueOf(genderStr) : null;

        User principal = User.builder()
                .id(Long.parseLong(claims.getSubject()))
                .username(claims.get("username", String.class))
                .name(claims.get("name", String.class))
                .role(Role.valueOf((String) authorities.iterator().next().getAuthority()))
                .gender(gender)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // User로부터 Authentication 생성
    public Authentication getAuthenticationFromUser(User user) {
        log.info("Creating Authentication from user: {}", user.getUsername());
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }

    // JWT 토큰 검증
    public boolean validateToken(String token) {
        token = stripBearerPrefix(token);
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            log.info("JWT token is valid.");
            return true;
        } catch (JwtException e) {
            log.error("JWT token is invalid: {}", e.getMessage());
        }
        return false;
    }

    // AccessToken 남은 만료 시간(ms) 조회
    public long getRemainingTime(String token) {
        try {
            token = stripBearerPrefix(token);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            return expiration.getTime() - now; // 남은 만료 시간
        } catch (JwtException e) {
            log.error("Error getting expiration from token: {}", e.getMessage());
            throw new InvalidTokenException("유효하지 않은 토큰입니다.");
        }
    }

    // ---------------------- Helper Methods ----------------------

    private User extractUserFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomOAuth2User oAuth2User) {
            log.info("Extracted user from CustomOAuth2User: {}", oAuth2User.getUser().getUsername());
            return oAuth2User.getUser();
        } else if (principal instanceof User user) {
            log.info("Extracted user from User: {}", user.getUsername());
            return user;
        } else {
            throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
        }
    }

    private String extractAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }

    private Date calculateTokenValidity(long validityInMilliseconds) {
        long now = (new Date()).getTime();
        return new Date(now + validityInMilliseconds);
    }

    private String buildToken(String userId, String username, String authorities, Date validity, Gender gender) {
        JwtBuilder builder = Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(validity);

        if (userId != null) {
            builder.setSubject(userId);
        }
        if (username != null) {
            builder.claim("username", username);
        }
        if (authorities != null) {
            builder.claim("auth", authorities);
        }
        if (gender != null) {
            builder.claim("gender", gender.name());
        }
        return builder.compact();
    }
    private Claims parseClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (JwtException e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    private String stripBearerPrefix(String token) {
        if (token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length());
        }
        return token;
    }
}
