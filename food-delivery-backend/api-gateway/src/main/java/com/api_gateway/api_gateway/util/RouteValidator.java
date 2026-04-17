package com.api_gateway.api_gateway.util;


import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Determines which routes are open (no JWT needed)
 * and which routes need authentication.
 */
@Component
public class RouteValidator {

    // Endpoints that don't require JWT authentication
    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator/health",
            "/actuator/info",
            "/fallback"
    );

    // GET requests to these paths are public
    public static final List<String> PUBLIC_GET_ENDPOINTS = List.of(
            "/api/restaurants"
    );

    /**
     * Returns true if the request requires authentication
     */
    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();
                String method = request.getMethod().name();

                // Check if it's an open endpoint (any method)
                for (String openEndpoint : OPEN_API_ENDPOINTS) {
                    if (path.startsWith(openEndpoint)) {
                        return false; // Not secured
                    }
                }

                // Check if it's a public GET endpoint
                if ("GET".equalsIgnoreCase(method)) {
                    for (String publicEndpoint : PUBLIC_GET_ENDPOINTS) {
                        if (path.startsWith(publicEndpoint)) {
                            return false; // Not secured
                        }
                    }
                }

                return true; // Secured - needs JWT
            };
}
