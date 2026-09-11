package com.order_service.kafka;

import com.order_service.dto.event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    private static final String ORDER_PLACED_TOPIC = "order.placed.test";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderEventProducer, "orderPlacedTopic", ORDER_PLACED_TOPIC);
    }

    @Test
    void publishOrderPlaced_sendsEventWithOrderIdAsKey() {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(100L)
                .userId(1L)
                .restaurantId(2L)
                .totalAmount(new BigDecimal("40.00"))
                .paymentMethod("CARD")
                .timestamp(LocalDateTime.now())
                .build();

        orderEventProducer.publishOrderPlaced(event);

        verify(kafkaTemplate).send(eq(ORDER_PLACED_TOPIC), eq("100"), eq(event));
    }
}
