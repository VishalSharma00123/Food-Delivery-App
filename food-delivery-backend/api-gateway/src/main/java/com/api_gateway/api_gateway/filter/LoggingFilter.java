package com.api_gateway.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logs every request passing through the gateway.
 * Useful for debugging and monitoring.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        log.info("============ GATEWAY REQUEST ============");
        log.info("Request ID  : {}", request.getId());
        log.info("Method      : {}", request.getMethod());
        log.info("Path        : {}", request.getURI().getPath());
        log.info("Query       : {}", request.getURI().getQuery());
        log.info("Remote Addr : {}", request.getRemoteAddress());
        log.info("==========================================");

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Response Status: {} | Duration: {}ms | Path: {}",
                    exchange.getResponse().getStatusCode(),
                    duration,
                    request.getURI().getPath());
        }));
    }

    @Override
    public int getOrder() {
        return -2; // Runs before JWT filter
    }
}