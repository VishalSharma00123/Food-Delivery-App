package com.payment_service.payment_service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cash on Delivery (COD) payment strategy.
 * COD always succeeds instantly — the actual collection happens at delivery time.
 */
@Slf4j
@Component
public class CodPaymentStrategy implements PaymentStrategy {

    @Override
    public String pay(BigDecimal amount) {
        log.info("Processing COD payment of ₹{}. Payment will be collected at delivery.", amount);
        // COD: no actual charge, generate a reference ID for tracking
        String transactionId = "COD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("COD confirmed. refId={}", transactionId);
        return transactionId;
    }

    @Override
    public String getMethodName() {
        return "COD";
    }
}
