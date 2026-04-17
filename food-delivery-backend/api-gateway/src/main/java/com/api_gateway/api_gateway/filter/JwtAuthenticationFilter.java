package com.api_gateway.api_gateway.filter;


import com.api_gateway.api_gateway.security.JwtService;
import com.api_gateway.api_gateway.util.RouteValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global JWT Authentication Filter for API Gateway.
 *
 * Flow:
 * 1. Check if route is open (no auth needed)
 * 2. Extract JWT from Authorization header
 * 3. Validate JWT
 * 4. Extract user info and pass as headers to downstream services
 * 5. If invalid, return 401
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final RouteValidator routeValidator;

    public JwtAuthenticationFilter(JwtService jwtService, RouteValidator routeValidator) {
        this.jwtService = jwtService;
        this.routeValidator = routeValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.debug("Gateway filter - {} {}", method, path);

        // Skip authentication for open endpoints
        if (!routeValidator.isSecured.test(request)) {
            log.debug("Open endpoint - skipping JWT validation: {} {}", method, path);
            return chain.filter(exchange);
        }

        // Check for Authorization header
        if (!request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
            log.warn("Missing Authorization header for: {} {}", method, path);
            return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format for: {} {}", method, path);
            return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // Validate token
        if (!jwtService.validateToken(token)) {
            log.warn("Invalid or expired JWT token for: {} {}", method, path);
            return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
        }

        // Extract user info from token
        try {
            String email = jwtService.extractEmail(token);
            Long userId = jwtService.extractUserId(token);
            List<String> roles = jwtService.extractRoles(token);

            log.debug("JWT valid - userId: {}, email: {}, roles: {}", userId, email, roles);

            // Add user info as headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Email", email)
                    .header("X-User-Roles", String.join(",", roles))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("Error extracting claims from JWT: {}", e.getMessage());
            return onError(exchange, "Error processing JWT token", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return -1; // High priority - runs before other filters
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message,
                java.time.LocalDateTime.now()
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}