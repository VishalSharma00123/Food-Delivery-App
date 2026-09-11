package com.api_gateway.api_gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class FallbackControllerTest {

    /**
     * ⚙️ What is this WebTestClient?
     *    private WebTestClient webTestClient;
     *
     * 👉 This is your fake client (like Postman, but inside code)
     *
     * */
    private WebTestClient webTestClient;

    /**
     *  setUp()
     * 👉 What’s happening:
     *
     *   You are NOT starting Spring Boot
     *   You are NOT using Tomcat
     *   You are just saying:
     *
     * “Hey, test this controller in isolation”
     *
     * */
    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(new FallbackController()).build();
    }

    /**
     * @Test
     * void authFallback_returnsServiceUnavailablePayload() {
     * */

    @Test
    void authFallback_returnsServiceUnavailablePayload() {

        ///  webTestClient.get().uri("/fallback/auth") = calling GET http://localhost/fallback/auth
        webTestClient.get().uri("/fallback/auth")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.path").isEqualTo("/api/auth");
    }

    @Test
    void paymentFallback_returnsServiceUnavailablePayload() {
        webTestClient.get().uri("/fallback/payment")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/api/payments");
    }

    @Test
    void notificationFallback_returnsServiceUnavailablePayload() {
        webTestClient.get().uri("/fallback/notification")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/api/notifications");
    }
}
