package com.payment_service.payment_service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Card payment strategy.
 * Simulates a card payment — always succeeds in dev/test mode.
 * In production, this would integrate with Stripe / Razorpay card APIs.
 */
@Slf4j
@Component
public class CardPaymentStrategy implements PaymentStrategy {

    @Override
    public String pay(BigDecimal amount) {
        log.info("Processing Card payment of ₹{}", amount);
        // Simulate card payment — generate a fake charge ID
        String transactionId = "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Card payment successful. transactionId={}", transactionId);
        return transactionId;
    }

    @Override
    public String getMethodName() {
        return "CARD";
    }
}
