package hwalibo.toilet.auth.jwt;

import hwalibo.toilet.auth.CustomOAuth2User;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

import static hwalibo.toilet.auth.jwt.JwtConstants.*;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;
    private final UserRepository userRepository;

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
        log.info("Creating access token for user: {}", authentication.getName());

        User user = extractUserFromAuthentication(authentication);
        String authorities = extractAuthorities(authentication);
        Date validity = calculateTokenValidity(this.accessTokenValidityInMilliseconds);

        return buildToken(user.getUsername(), authorities, validity);
    }

    // Refresh Token 생성
    public String createRefreshToken() {
        log.info("Creating refresh token");
        Date validity = calculateTokenValidity(this.refreshTokenValidityInMilliseconds);
        return buildToken(null, null, validity);
    }

    // Authentication 객체 가져오기
    public Authentication getAuthentication(String token) {
        log.info("Parsing JWT token to get authentication");

        Claims claims = parseClaimsFromToken(token);
        String[] subjectParts = extractSubjectParts(claims.getSubject());
        User user = findUser(subjectParts[0], subjectParts[1]);

        return new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities());
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
    public long getExpiration(String token) {
        try {
            token = stripBearerPrefix(token); // "Bearer " 제거
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();

            return expiration.getTime() - now; // 남은 만료 시간 (ms)
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

    private String buildToken(String username, String authorities, Date validity) {
        JwtBuilder builder = Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(validity);

        if (username != null) {
            builder.setSubject(username)
                    .claim("auth", authorities);
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

    private String[] extractSubjectParts(String subject) {
        String[] parts = subject.split("_", 2);
        if (parts.length != 2) {
            log.error("Invalid subject in token: {}", subject);
            throw new IllegalArgumentException("Invalid subject in token: " + subject);
        }
        return parts;
    }

    private User findUser(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + provider + "_" + providerId));
    }

    private String stripBearerPrefix(String token) {
        if (token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length());
        }
        return token;
    }
}

