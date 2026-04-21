package com.payment_service.payment_service.kafka;

import com.payment_service.payment_service.dto.event.OrderPlacedEvent;
import com.payment_service.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    /**
     * Listens to the order.placed topic and triggers payment processing.
     * This is the entry point for the entire payment flow.
     */
    @KafkaListener(
        topics = "${kafka.topics.order-placed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent: orderId={}, userId={}, amount={}, method={}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount(), event.getPaymentMethod());
        paymentService.processPayment(event);
    }
}
