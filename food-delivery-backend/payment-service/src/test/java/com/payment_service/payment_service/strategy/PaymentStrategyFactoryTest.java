package com.payment_service.payment_service.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentStrategyFactoryTest {

    private PaymentStrategy upi;
    private PaymentStrategy card;
    private PaymentStrategyFactory factory;

    @BeforeEach
    void setUp() {
        upi = mock(PaymentStrategy.class);
        when(upi.getMethodName()).thenReturn("UPI");
        card = mock(PaymentStrategy.class);
        when(card.getMethodName()).thenReturn("CARD");
        factory = new PaymentStrategyFactory(List.of(upi, card));
    }

    @Test
    void getStrategy_returnsMatchingStrategy_caseInsensitive() {
        assertSame(upi, factory.getStrategy("upi"));
        assertSame(card, factory.getStrategy("CARD"));
    }

    @Test
    void getStrategy_throwsWhenUnsupported() {
        assertThrows(IllegalArgumentException.class, () -> factory.getStrategy("WIRE"));
    }
}
