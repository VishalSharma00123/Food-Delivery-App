package com.order_service.kafka;

import com.order_service.dto.event.PaymentConfirmedEvent;
import com.order_service.dto.event.PaymentFailedEvent;
import com.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    void onPaymentConfirmed_updatesOrderToConfirmed() {
        PaymentConfirmedEvent event = PaymentConfirmedEvent.builder()
                .paymentId(1L)
                .orderId(42L)
                .userId(9L)
                .amount(new BigDecimal("25.00"))
                .paymentMethod("UPI")
                .transactionId("TX-1")
                .timestamp(LocalDateTime.now())
                .build();

        paymentEventConsumer.onPaymentConfirmed(event);

        verify(orderService).updateOrderStatus(42L, "CONFIRMED");
    }

    @Test
    void onPaymentFailed_updatesOrderToPaymentFailed() {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(2L)
                .orderId(43L)
                .userId(9L)
                .amount(new BigDecimal("10.00"))
                .reason("Declined")
                .timestamp(LocalDateTime.now())
                .build();

        paymentEventConsumer.onPaymentFailed(event);

        verify(orderService).updateOrderStatus(43L, "PAYMENT_FAILED");
    }
}
