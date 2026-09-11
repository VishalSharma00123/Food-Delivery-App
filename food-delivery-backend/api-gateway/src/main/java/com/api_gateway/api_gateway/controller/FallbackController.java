package com.api_gateway.api_gateway.controller;

import com.api_gateway.api_gateway.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback controller when downstream services are unavailable.
 * Circuit breaker redirects here when services are down.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public ResponseEntity<ApiErrorResponse> authFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of(
                        "Auth Service is currently unavailable. Please try again later.",
                        "/api/auth",
                        503));
    }

    @RequestMapping("/restaurant")
    public ResponseEntity<ApiErrorResponse> restaurantFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of(
                        "Restaurant Service is currently unavailable. Please try again later.",
                        "/api/restaurants",
                        503));
    }

    @RequestMapping("/order")
    public ResponseEntity<ApiErrorResponse> orderFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of(
                        "Order Service is currently unavailable. Please try again later.",
                        "/api/orders",
                        503));
    }

    @RequestMapping("/user")
    public ResponseEntity<ApiErrorResponse> userFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of(
                        "User Service is currently unavailable. Please try again later.",
                        "/api/users",
                        503));
    }

    @RequestMapping("/payment")
    public ResponseEntity<ApiErrorResponse> paymentFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of(
                        "Payment Service is currently unavailable. Please try again later.",
                        "/api/payments",
                        503));
    }

    @RequestMapping("/notification")
    public ResponseEntity<ApiErrorResponse> notificationFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of(
                        "Notification Service is currently unavailable. Please try again later.",
                        "/api/notifications",
                        503));
    }
}
