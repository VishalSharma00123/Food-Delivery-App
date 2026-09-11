package com.payment_service.payment_service.security;

import com.fooddelivery.rbac.AbstractJwtAuthenticationFilter;
import com.fooddelivery.rbac.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class PaymentJwtAuthenticationFilter extends AbstractJwtAuthenticationFilter {

    public PaymentJwtAuthenticationFilter(JwtService jwtService) {
        super(jwtService);
    }

    @Override
    protected boolean shouldBypassJwtParsing(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }
}
