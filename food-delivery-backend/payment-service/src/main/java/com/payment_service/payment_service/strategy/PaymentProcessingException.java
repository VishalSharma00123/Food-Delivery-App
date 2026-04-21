package com.payment_service.payment_service.strategy;

/**
 * Thrown when any payment strategy encounters a processing error.
 */
public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message) {
        super(message);
    }
}
