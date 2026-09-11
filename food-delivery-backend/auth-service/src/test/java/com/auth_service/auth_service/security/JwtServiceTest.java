package com.auth_service.auth_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L);
    }

    @Test
    void generateToken_embedsClaims_andValidatesForSubject() {
        String token = jwtService.generateToken("a@b.com", 5L, List.of("ADMIN", "USER"));

        assertEquals("a@b.com", jwtService.extractUsername(token));
        assertEquals(5L, jwtService.extractUserId(token));
        assertEquals(List.of("ADMIN", "USER"), jwtService.extractRoles(token));
        assertTrue(jwtService.isTokenValid(token, "a@b.com"));
    }

    @Test
    void isTokenValid_returnsFalse_forWrongEmail() {
        String token = jwtService.generateToken("a@b.com", 1L, List.of("ADMIN"));

        assertFalse(jwtService.isTokenValid(token, "other@b.com"));
    }
}
