package com.api_gateway.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate Limiter Configuration using Redis + Token Bucket algorithm.
 *
 * Spring Cloud Gateway's built-in RequestRateLimiter uses Redis to implement
 * the Token Bucket algorithm:
 *   - replenishRate  : tokens added per second (steady-state RPS)
 *   - burstCapacity  : max tokens in the bucket (burst headroom)
 *   - requestedTokens: tokens consumed per request (default 1)
 *
 * KeyResolvers decide HOW to identify a client:
 *   - ipKeyResolver     : per client IP  (unauthenticated / public routes)
 *   - userKeyResolver   : per logged-in user via X-User-Id header
 *   - apiKeyKeyResolver : per API key header (future use)
 */
@Configuration
public class RateLimiterConfig {

    // -----------------------------------------------------------------------
    // Key Resolvers
    // -----------------------------------------------------------------------

    /**
     * Resolves the rate-limit key by client IP address.
     * Falls back to "unknown" when the address cannot be determined.
     * Used for public / unauthenticated routes.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");

            if (forwardedFor != null && !forwardedFor.isBlank()) {
                // Take the first IP when there are multiple proxies
                return Mono.just(forwardedFor.split(",")[0].trim());
            }

            return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                    .map(addr -> addr.getAddress().getHostAddress())
                    .defaultIfEmpty("unknown");
        };
    }

    /**
     * Resolves the rate-limit key by authenticated user ID.
     * The JwtAuthenticationFilter populates the X-User-Id header.
     * Falls back to the client IP when the header is absent.
     * Used for secured / authenticated routes.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }

            // Fallback to IP for unauthenticated requests on secured routes
            String forwardedFor = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return Mono.just("ip:" + forwardedFor.split(",")[0].trim());
            }

            return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                    .map(addr -> "ip:" + addr.getAddress().getHostAddress())
                    .defaultIfEmpty("ip:unknown");
        };
    }

    // -----------------------------------------------------------------------
    // RedisRateLimiter instances (Token Bucket per route category)
    // -----------------------------------------------------------------------

    /**
     * Auth endpoints  – lower limits to deter brute-force attacks.
     * 10 req/s steady, burst up to 20.
     */
    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Public read endpoints (e.g. GET /api/restaurants/**).
     * 50 req/s steady, burst up to 100.
     */
    @Bean
    public RedisRateLimiter publicReadRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }

    /**
     * Authenticated write / transactional endpoints
     * (orders, payments, restaurant mutations).
     * 30 req/s steady, burst up to 60.
     */
    @Bean
    public RedisRateLimiter authenticatedRateLimiter() {
        return new RedisRateLimiter(30, 60, 1);
    }

    /**
     * Notification service – internal use, generous limits.
     * 20 req/s steady, burst up to 40.
     */
    @Bean
    public RedisRateLimiter notificationRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }
}
