package com.notification_service.notification_service.security;

import com.fooddelivery.rbac.AbstractJwtAuthenticationFilter;
import com.fooddelivery.rbac.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class NotificationJwtAuthenticationFilter extends AbstractJwtAuthenticationFilter {

    public NotificationJwtAuthenticationFilter(JwtService jwtService) {
        super(jwtService);
    }

    @Override
    protected boolean shouldBypassJwtParsing(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }
}
