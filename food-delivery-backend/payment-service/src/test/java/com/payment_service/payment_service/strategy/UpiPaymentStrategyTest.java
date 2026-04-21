package com.payment_service.payment_service.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpiPaymentStrategyTest {

    private final UpiPaymentStrategy strategy = new UpiPaymentStrategy();

    @Test
    void pay_returnsTxnIdStartingWithUpiPrefix() {
        String txn = strategy.pay(new BigDecimal("10.00"));

        assertNotNull(txn);
        assertTrue(txn.startsWith("UPI-"));
        assertEquals(12, txn.length());
    }

    @Test
    void getMethodName_isUpi() {
        assertEquals("UPI", strategy.getMethodName());
    }
}
