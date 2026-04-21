package com.payment_service.payment_service.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodPaymentStrategyTest {

    private final CodPaymentStrategy strategy = new CodPaymentStrategy();

    @Test
    void pay_returnsTxnIdStartingWithCodPrefix() {
        String txn = strategy.pay(BigDecimal.ONE);

        assertNotNull(txn);
        assertTrue(txn.startsWith("COD-"));
        assertEquals(12, txn.length());
    }

    @Test
    void getMethodName_isCod() {
        assertEquals("COD", strategy.getMethodName());
    }
}
