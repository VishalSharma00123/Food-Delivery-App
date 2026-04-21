package com.payment_service.payment_service.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardPaymentStrategyTest {

    private final CardPaymentStrategy strategy = new CardPaymentStrategy();

    @Test
    void pay_returnsTxnIdStartingWithCardPrefix() {
        String txn = strategy.pay(new BigDecimal("25.50"));

        assertNotNull(txn);
        assertTrue(txn.startsWith("CARD-"));
        assertEquals(13, txn.length());
    }

    @Test
    void getMethodName_isCard() {
        assertEquals("CARD", strategy.getMethodName());
    }
}
