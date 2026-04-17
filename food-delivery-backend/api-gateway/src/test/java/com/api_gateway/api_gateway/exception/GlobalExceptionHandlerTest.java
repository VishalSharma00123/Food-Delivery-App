package com.api_gateway.api_gateway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void testHandleResponseStatusException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
        
        exceptionHandler.handle(exchange, ex).block();
        
        // Ensure the response got the modified status natively
        assertNotNull(exchange.getResponse().getStatusCode());
    }
}
