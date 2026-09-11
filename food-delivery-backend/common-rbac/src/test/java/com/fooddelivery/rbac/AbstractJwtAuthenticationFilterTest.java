package com.fooddelivery.rbac;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractJwtAuthenticationFilterTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String validToken() {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 120_000);
        return Jwts.builder()
                .setSubject("jwt-filter@example.com")
                .claim("userId", 42L)
                .claim("roles", List.of(AppRoles.USER))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private AbstractJwtAuthenticationFilter filter() {
        JwtService jwt = new JwtService(SECRET);
        return new AbstractJwtAuthenticationFilter(jwt) {
            @Override
            protected boolean shouldBypassJwtParsing(HttpServletRequest request) {
                return "/actuator/health".equals(request.getRequestURI());
            }
        };
    }

    @Test
    void bypassPath_continuesWithoutAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void missingBearer_continuesWithoutAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void gatewayIdentityHeaders_setsSecurityContextWithoutBearer() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        req.addHeader("X-User-Id", "42");
        req.addHeader("X-User-Email", "gateway@example.com");
        req.addHeader("X-User-Roles", "CUSTOMER,USER");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("gateway@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(42L, SecurityContextHolder.getContext().getAuthentication().getDetails());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> AppRoles.AUTHORITY_CUSTOMER.equals(a.getAuthority())));
    }

    @Test
    void xAccessTokenHeader_setsSecurityContextWhenAuthorizationMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        req.addHeader("X-Access-Token", "Bearer " + validToken());
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(42L, SecurityContextHolder.getContext().getAuthentication().getDetails());
    }

    @Test
    void invalidToken_returnsUnauthorized() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        req.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertTrue(res.getContentAsString().contains("Invalid or expired token"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validBearer_setsSecurityContext() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        req.addHeader("Authorization", "Bearer " + validToken());
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("jwt-filter@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(42L, SecurityContextHolder.getContext().getAuthentication().getDetails());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> AppRoles.AUTHORITY_USER.equals(a.getAuthority())));
    }
}
