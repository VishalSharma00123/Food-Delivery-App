package com.api_gateway.api_gateway.config;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route configuration (alternative to YAML).
 * Currently routes are defined in application.yml.
 *
 * This class provides fallback routes.
 */
@Configuration
public class GatewayConfig {

    /**
     * Fallback routes when downstream services are down.
     */
    @Bean
    public RouteLocator fallbackRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("fallback-route", r -> r
                        .path("/fallback/**")
                        .uri("no://op"))
                .build();
    }
}