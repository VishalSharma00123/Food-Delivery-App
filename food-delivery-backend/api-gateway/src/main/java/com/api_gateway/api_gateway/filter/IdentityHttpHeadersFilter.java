package com.api_gateway.api_gateway.filter;

import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Ensures identity headers survive {@link org.springframework.cloud.gateway.filter.NettyRoutingFilter}'s
 * request header filtering pipeline (the last step before the downstream HTTP call).
 */
@Component
public class IdentityHttpHeadersFilter implements HttpHeadersFilter, Ordered {

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(input);

        String authHeader = exchange.getAttribute(IdentityHeadersBridge.AUTH_HEADER);
        String userId = exchange.getAttribute(IdentityHeadersBridge.USER_ID);
        String email = exchange.getAttribute(IdentityHeadersBridge.USER_EMAIL);
        String roles = exchange.getAttribute(IdentityHeadersBridge.USER_ROLES);

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
        return headers;
    }

    @Override
    public boolean supports(Type type) {
        return Type.REQUEST.equals(type);
    }

    @Override
    public int getOrder() {
        // After hop-by-hop removal so our identity headers are not dropped later.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
