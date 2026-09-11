package com.api_gateway.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Re-applies JWT identity headers immediately before {@link NettyRoutingFilter}.
 * Earlier mutations can be lost depending on filter order / body caching.
 */
@Component
public class IdentityForwardingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getAttribute(IdentityHeadersBridge.AUTH_HEADER);
        String userId = exchange.getAttribute(IdentityHeadersBridge.USER_ID);
        if (authHeader == null && userId == null) {
            return chain.filter(exchange);
        }

        String email = exchange.getAttribute(IdentityHeadersBridge.USER_EMAIL);
        String roles = exchange.getAttribute(IdentityHeadersBridge.USER_ROLES);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    if (authHeader != null) {
                        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
                        headers.set("X-Access-Token", authHeader);
                    }
                    if (userId != null) {
                        headers.set("X-User-Id", userId);
                    }
                    if (email != null) {
                        headers.set("X-User-Email", email);
                    }
                    if (roles != null) {
                        headers.set("X-User-Roles", roles);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        // Just before the HTTP client sends the downstream request.
        return NettyRoutingFilter.ORDER - 1;
    }
}
