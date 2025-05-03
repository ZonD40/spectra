package spectra.ru.users.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spectra.ru.users.api.dto.user.JwtAuthenticationDto;

import java.time.Duration;
import java.util.Date;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    String jwtSecret;

    @Value("${jwt.expiration}")
    long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    long jwtRefreshExpiration;

    final RedisTemplate<String, String> blacklistRedisTemplate;

    static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

    public JwtAuthenticationDto generateAuthToken(String email, Long id) {
        return JwtAuthenticationDto
                .builder()
                .token(generateToken(email, id, jwtExpiration, "access"))
                .refreshToken(generateToken(email, id, jwtRefreshExpiration, "refresh"))
                .build();

    }

    public void addTokenToBlacklist(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        blacklistRedisTemplate.opsForValue().set(key, "true", Duration.ofMillis(jwtRefreshExpiration));
    }

    public boolean isTokenInBlacklist(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        return blacklistRedisTemplate.hasKey(key);
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String generateToken(String email, Long id, long expiration, String type) {
        return Jwts.builder()
                .subject(email)
                .issuer("spectra")
                .claim("id", id)
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }



}
