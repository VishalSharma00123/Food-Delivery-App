package com.payment_service.payment_service.strategy;

import java.math.BigDecimal;

/**
 * Strategy Pattern — defines the contract for all payment methods.
 * Each payment method (UPI, Card, COD) implements this interface independently.
 */
public interface PaymentStrategy {

    /**
     * Process the payment for the given amount.
     *
     * @param amount the amount to charge
     * @return a unique transaction ID on success
     * @throws PaymentProcessingException if payment fails
     */
    String pay(BigDecimal amount);

    /**
     * Returns the name of this payment strategy (e.g., "UPI").
     */
    String getMethodName();
}
