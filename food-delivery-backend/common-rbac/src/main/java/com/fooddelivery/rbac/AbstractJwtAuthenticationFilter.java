package com.fooddelivery.rbac;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    protected AbstractJwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    protected abstract boolean shouldBypassJwtParsing(HttpServletRequest request);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (shouldBypassJwtParsing(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = firstBearerHeader(request);
        if (authHeader != null) {
            final String token = authHeader.substring(7);
            if (!jwtService.validateToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getOutputStream().write(
                        "{\"success\":false,\"message\":\"Invalid or expired token\"}".getBytes(StandardCharsets.UTF_8)
                );
                return;
            }

            String username = jwtService.extractUsername(token);
            Long userId = jwtService.extractUserId(token);
            // Gateway may already forward the resolved id when claim parsing differs across services.
            if (userId == null) {
                userId = parseUserIdHeader(request.getHeader("X-User-Id"));
            }
            List<String> roles = jwtService.extractRoles(token);
            if (roles == null) {
                roles = List.of();
            }
            setAuthentication(username, userId, roles);
            filterChain.doFilter(request, response);
            return;
        }

        // API Gateway validates JWT then forwards X-User-* identity headers. Some Gateway
        // versions strip Authorization before the downstream hop — trust those headers.
        if (authenticateFromGatewayHeaders(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String firstBearerHeader(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization;
        }
        // Gateway copies Bearer here when Authorization is treated as a sensitive header.
        String accessToken = request.getHeader("X-Access-Token");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            return accessToken;
        }
        return null;
    }

    private boolean authenticateFromGatewayHeaders(HttpServletRequest request) {
        Long userId = parseUserIdHeader(request.getHeader("X-User-Id"));
        if (userId == null) {
            return false;
        }
        String email = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Roles");
        List<String> roles = List.of();
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            roles = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .toList();
        }
        String principal = (email != null && !email.isBlank()) ? email.trim() : String.valueOf(userId);
        setAuthentication(principal, userId, roles);
        return true;
    }

    private static Long parseUserIdHeader(String headerUserId) {
        if (headerUserId == null || headerUserId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(headerUserId.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void setAuthentication(String username, Long userId, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SecurityRoleUtils::toAuthority)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
