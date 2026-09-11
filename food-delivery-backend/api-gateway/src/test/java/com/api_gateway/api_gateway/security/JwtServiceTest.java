package com.api_gateway.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "12345678901234567890123456789012";

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
    }

    @Test
    void validateToken_acceptsTokenSignedWithSameSecret() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .setSubject("user@example.com")
                .claim("userId", 9L)
                .claim("roles", List.of("CUSTOMER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertTrue(jwtService.validateToken(token));
        assertEquals("user@example.com", jwtService.extractEmail(token));
        assertEquals(9L, jwtService.extractUserId(token));
        assertEquals(List.of("CUSTOMER"), jwtService.extractRoles(token));
    }

    @Test
    void validateToken_rejectsMalformedToken() {
        assertFalse(jwtService.validateToken("not-a-jwt"));
    }
}
