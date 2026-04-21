package com.payment_service.payment_service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * UPI payment strategy.
 * Simulates a UPI payment — always succeeds in dev/test mode.
 * In production, this would integrate with a UPI payment gateway (e.g., Razorpay UPI API).
 */
@Slf4j
@Component
public class UpiPaymentStrategy implements PaymentStrategy {

    @Override
    public String pay(BigDecimal amount) {
        log.info("Processing UPI payment of ₹{}", amount);
        // Simulate UPI payment — generate a fake transaction ID
        String transactionId = "UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("UPI payment successful. transactionId={}", transactionId);
        return transactionId;
    }

    @Override
    public String getMethodName() {
        return "UPI";
    }
}
