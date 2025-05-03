package spectra.ru.users.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import spectra.ru.users.api.dto.user.JwtAuthenticationDto;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    RedisTemplate<String, String> blacklistRedisTemplate;

    @Mock
    private ValueOperations<String, String> blacklistValueOperations;

    final String jwtSecret = "veeeeeeeeeeeeeeeeeeryLongSecretKey";


    final long jwtExpiration = 86400000;


    final long jwtRefreshExpiration = 1209600000;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(blacklistRedisTemplate);
        ReflectionTestUtils.setField(jwtService, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", jwtExpiration);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpiration", jwtRefreshExpiration);
    }

    @Test
    void generateAuthTokenTest() {
        JwtAuthenticationDto result = jwtService.generateAuthToken("test@mail.ru", 1L);

        assertNotNull(result.getToken());
        assertNotNull(result.getRefreshToken());

        var claims = parseToken(result.getToken());
        var refreshClaims = parseToken(result.getRefreshToken());

        assertEquals("test@mail.ru", claims.getSubject());
        assertEquals("spectra", claims.getIssuer());
        assertEquals(1L, claims.get("id", Long.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        assertEquals("test@mail.ru", refreshClaims.getSubject());
        assertEquals("spectra", refreshClaims.getIssuer());
        assertEquals(1L, refreshClaims.get("id", Long.class));
        assertNotNull(refreshClaims.getIssuedAt());
        assertNotNull(refreshClaims.getExpiration());

        long tokenExpirationTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        long refreshTokenExpirationTime = refreshClaims.getExpiration().getTime() - refreshClaims.getIssuedAt().getTime();

        assertEquals(jwtExpiration, tokenExpirationTime, 1000);
        assertEquals(jwtRefreshExpiration, refreshTokenExpirationTime, 1000);
    }

    @Test
    void addTokenToBlacklistTest() {
        String token = "token";

        when(blacklistRedisTemplate.opsForValue()).thenReturn(blacklistValueOperations);

        jwtService.addTokenToBlacklist(token);

        verify(blacklistValueOperations).set("blacklist:token:" + token, "true", Duration.ofMillis(jwtRefreshExpiration));
    }

    @Test
    void isTokenInBlacklistTest() {
        String token = "token";

        when(blacklistRedisTemplate.hasKey("blacklist:token:" + token)).thenReturn(true);

        boolean result = jwtService.isTokenInBlacklist(token);

        assertTrue(result);
    }

    @Test
    void getClaimsTest() {
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ2aXAubGVvbnRldjIwMDJAbWFpbC5ydSIsImlzcyI6InNwZWN0cmEiLCJpZCI6MTMsInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3NDYyODQwMTksImV4cCI6MTc0NjI4NzYxOX0.3bd1eYd0vkNBbIiLXDqfzZ_eFsAMqGDB2uUj2LYVLDE";

        Claims result = jwtService.getClaims(token);

        assertNotNull(result);
        assertEquals("vip.leontev2002@mail.ru", result.getSubject());
        assertEquals(13L, result.get("id", Long.class));
        assertNotNull(result.getIssuedAt());
        assertEquals("spectra", result.getIssuer());
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
