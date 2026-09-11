package com.user_service.security;

import com.fooddelivery.rbac.AbstractJwtAuthenticationFilter;
import com.fooddelivery.rbac.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class UserJwtAuthenticationFilter extends AbstractJwtAuthenticationFilter {

    public UserJwtAuthenticationFilter(JwtService jwtService) {
        super(jwtService);
    }

    @Override
    protected boolean shouldBypassJwtParsing(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }
}
