package com.payment_service.payment_service.config;

import com.payment_service.payment_service.security.PaymentJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PaymentJwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(PaymentJwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    boolean hasAuthorization = request.getHeader("Authorization") != null
                            && !request.getHeader("Authorization").isBlank();
                    boolean hasUserId = request.getHeader("X-User-Id") != null
                            && !request.getHeader("X-User-Id").isBlank();
                    String body = "{\"success\":false,\"message\":\"Authentication required\""
                            + ",\"hasAuthorization\":" + hasAuthorization
                            + ",\"hasUserIdHeader\":" + hasUserId + "}";
                    response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getOutputStream().write(
                            "{\"success\":false,\"message\":\"Access denied\"}"
                                    .getBytes(StandardCharsets.UTF_8)
                    );
                })
        );
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                // Digits-only paymentId so /razorpay/** is never mistaken for refund.
                .requestMatchers(HttpMethod.POST, "/api/payments/{paymentId:\\d+}/refund").hasRole("ADMIN")
                .requestMatchers("/api/payments/**").authenticated()
                .anyRequest().authenticated()
        );
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
