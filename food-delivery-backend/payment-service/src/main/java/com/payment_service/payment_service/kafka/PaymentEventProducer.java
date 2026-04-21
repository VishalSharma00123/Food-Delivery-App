package com.payment_service.payment_service.kafka;

import com.payment_service.payment_service.dto.event.PaymentConfirmedEvent;
import com.payment_service.payment_service.dto.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-confirmed}")
    private String paymentConfirmedTopic;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    public void publishPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Publishing PaymentConfirmedEvent: orderId={}, txnId={}",
                event.getOrderId(), event.getTransactionId());
        kafkaTemplate.send(paymentConfirmedTopic, String.valueOf(event.getOrderId()), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.warn("Publishing PaymentFailedEvent: orderId={}, reason={}",
                event.getOrderId(), event.getReason());
        kafkaTemplate.send(paymentFailedTopic, String.valueOf(event.getOrderId()), event);
    }
}
