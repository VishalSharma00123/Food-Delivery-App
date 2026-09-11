package com.restaurent_service.restaurent_service.security;

import com.fooddelivery.rbac.AbstractJwtAuthenticationFilter;
import com.fooddelivery.rbac.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationFilter extends AbstractJwtAuthenticationFilter {

    public JwtAuthenticationFilter(JwtService jwtService) {
        super(jwtService);
    }

    @Override
    protected boolean shouldBypassJwtParsing(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/public/")) {
            return true;
        }
        return "GET".equalsIgnoreCase(request.getMethod()) && uri.startsWith("/api/restaurants");
    }
}
