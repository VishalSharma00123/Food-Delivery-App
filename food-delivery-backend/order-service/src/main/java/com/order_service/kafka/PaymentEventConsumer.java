package com.order_service.kafka;

import com.order_service.dto.event.PaymentConfirmedEvent;
import com.order_service.dto.event.PaymentFailedEvent;
import com.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
        topics = "${kafka.topics.payment-confirmed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Received PaymentConfirmedEvent: orderId={}, transactionId={}",
                event.getOrderId(), event.getTransactionId());
        orderService.updateOrderStatus(event.getOrderId(), "CONFIRMED");
    }

    @KafkaListener(
        topics = "${kafka.topics.payment-failed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("Received PaymentFailedEvent: orderId={}, reason={}",
                event.getOrderId(), event.getReason());
        orderService.updateOrderStatus(event.getOrderId(), "PAYMENT_FAILED");
    }
}
