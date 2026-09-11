package com.fooddelivery.rbac;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private JwtService jwtService() {
        return new JwtService(SECRET);
    }

    private String signedToken(Date issuedAt, Date expiresAt, List<String> roles) {
        return Jwts.builder()
                .setSubject("user@example.com")
                .claim("userId", 100L)
                .claim("roles", roles)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void validateToken_validToken_returnsTrueAndExtractsClaims() {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 120_000);
        String token = signedToken(now, exp, List.of(AppRoles.USER, AppRoles.ADMIN));
        JwtService jwt = jwtService();

        assertTrue(jwt.validateToken(token));
        assertEquals("user@example.com", jwt.extractUsername(token));
        assertEquals(100L, jwt.extractUserId(token));
        assertEquals(List.of(AppRoles.USER, AppRoles.ADMIN), jwt.extractRoles(token));
    }

    @Test
    void validateToken_expired_returnsFalse() {
        Date issued = new Date(System.currentTimeMillis() - 120_000);
        Date expired = new Date(System.currentTimeMillis() - 60_000);
        String token = signedToken(issued, expired, List.of(AppRoles.USER));
        JwtService jwt = jwtService();

        assertFalse(jwt.validateToken(token));
    }

    @Test
    void validateToken_wrongSecret_returnsFalse() {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 120_000);
        String token = signedToken(now, exp, List.of(AppRoles.USER));
        JwtService otherKey = new JwtService("98765432109876543210987654321098");

        assertFalse(otherKey.validateToken(token));
    }

    @Test
    void extractRoles_whenMissing_returnsNull() {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 120_000);
        String token = Jwts.builder()
                .setSubject("only@subject.com")
                .claim("userId", 1L)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
        JwtService jwt = jwtService();

        assertTrue(jwt.validateToken(token));
        assertNull(jwt.extractRoles(token));
    }
}
