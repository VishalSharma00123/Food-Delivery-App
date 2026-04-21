package com.api_gateway.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-Memory Sliding-Window Rate Limiting Filter.
 *
 * This filter acts as a LAST-RESORT guard when Redis is unavailable.
 * When Redis is up, the per-route {@code RequestRateLimiter} filters defined
 * in application.yaml take precedence.
 *
 * Algorithm  : Sliding Window Log (per client IP)
 * Limit      : {@value MAX_REQUESTS} requests per {@value WINDOW_MS} ms
 * Order      : -3 (runs before JwtAuthenticationFilter at -1)
 *
 * Response headers added on every request:
 *   X-RateLimit-Limit     – configured maximum
 *   X-RateLimit-Remaining – remaining requests in the current window
 *   X-RateLimit-Reset     – epoch second when the window resets
 *   Retry-After           – seconds to wait on 429 responses
 */
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** Maximum requests allowed per client IP within the sliding window. */
    private static final int MAX_REQUESTS = 100;

    /** Sliding window duration in milliseconds (60 seconds). */
    private static final long WINDOW_MS = 60_000L;

    /**
     * Per-IP sliding window log.
     * Each entry holds the timestamps (ms) of requests made in the window.
     */
    private final Map<String, Deque<Long>> windowLog = new ConcurrentHashMap<>();

    /** Tracks the size of each client's window for fast {@code remaining} calculation. */
    private final Map<String, AtomicLong> windowSize = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String clientIp = resolveClientIp(exchange);
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        // Get or create the per-IP deque
        Deque<Long> timestamps = windowLog.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());
        AtomicLong counter   = windowSize.computeIfAbsent(clientIp, k -> new AtomicLong(0));

        // Evict timestamps outside the current window
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
                counter.decrementAndGet();
            }

            long current = counter.get();
            long remaining = Math.max(0, MAX_REQUESTS - current);
            long resetEpochSec = (now + WINDOW_MS) / 1000;

            // Attach rate-limit headers to every response
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit",     String.valueOf(MAX_REQUESTS));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            exchange.getResponse().getHeaders().add("X-RateLimit-Reset",     String.valueOf(resetEpochSec));

            if (current >= MAX_REQUESTS) {
                long retryAfterSec = (WINDOW_MS - (now - timestamps.peekFirst())) / 1000 + 1;
                log.warn("Rate limit exceeded for IP: {} ({}/{} req/min)", clientIp, current, MAX_REQUESTS);
                return onRateLimitExceeded(exchange, retryAfterSec);
            }

            // Record this request
            timestamps.addLast(now);
            counter.incrementAndGet();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Runs before JwtAuthenticationFilter (-1) and LoggingFilter (-2)
        return -3;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange, long retryAfterSec) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add("Retry-After", String.valueOf(retryAfterSec));

        String body = String.format(
                "{\"success\":false,\"status\":429,"
                + "\"message\":\"Rate limit exceeded. Please retry after %d second(s).\","
                + "\"retryAfter\":%d}",
                retryAfterSec, retryAfterSec
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}