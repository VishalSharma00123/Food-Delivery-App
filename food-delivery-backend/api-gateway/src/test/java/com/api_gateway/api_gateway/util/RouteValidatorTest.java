package com.api_gateway.api_gateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteValidatorTest {

    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
    }

    @Test
    void openAuthLoginPath_doesNotRequireJwt() {
        var request = MockServerHttpRequest.get("/api/auth/login").build();

        assertFalse(routeValidator.isSecured.test(request));
    }

    @Test
    void actuatorHealth_doesNotRequireJwt() {
        var request = MockServerHttpRequest.get("/actuator/health").build();

        assertFalse(routeValidator.isSecured.test(request));
    }

    @Test
    void publicRestaurantGet_doesNotRequireJwt() {
        var request = MockServerHttpRequest.get("/api/restaurants").build();

        assertFalse(routeValidator.isSecured.test(request));
    }

    @Test
    void publicRestaurantMenuGet_doesNotRequireJwt() {
        var request = MockServerHttpRequest.get("/api/public/restaurants/1/menu").build();

        assertFalse(routeValidator.isSecured.test(request));
    }

    @Test
    void publicMediaGet_doesNotRequireJwt() {
        var request = MockServerHttpRequest.get("/api/public/media/menu/1/photo.jpg").build();

        assertFalse(routeValidator.isSecured.test(request));
    }

    @Test
    void restaurantPost_requiresJwt() {
        var request = MockServerHttpRequest.method(HttpMethod.POST, "/api/restaurants").build();

        assertTrue(routeValidator.isSecured.test(request));
    }

    @Test
    void ordersPath_requiresJwt() {
        var request = MockServerHttpRequest.get("/api/orders/1").build();

        assertTrue(routeValidator.isSecured.test(request));
    }

    @Test
    void optionsPreflight_doesNotRequireJwt() {
        var request = MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/payments/razorpay/initiate").build();

        assertFalse(routeValidator.isSecured.test(request));
    }
}
