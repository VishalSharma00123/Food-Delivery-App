package com.order_service.security;

import com.fooddelivery.rbac.AbstractJwtAuthenticationFilter;
import com.fooddelivery.rbac.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class OrderJwtAuthenticationFilter extends AbstractJwtAuthenticationFilter {

    public OrderJwtAuthenticationFilter(JwtService jwtService) {
        super(jwtService);
    }

    @Override
    protected boolean shouldBypassJwtParsing(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/");
    }
}
