package com.notification_service.notification_service.kafka;

import com.notification_service.notification_service.dto.event.OrderPlacedEvent;
import com.notification_service.notification_service.dto.event.PaymentConfirmedEvent;
import com.notification_service.notification_service.dto.event.PaymentFailedEvent;
import com.notification_service.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

	private final NotificationService notificationService;

	@KafkaListener(
			topics = "${kafka.topics.order-placed}",
			groupId = "${spring.kafka.consumer.group-id}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void onOrderPlaced(OrderPlacedEvent event) {
		log.info("Received OrderPlacedEvent: orderId={}, userId={}", event.getOrderId(), event.getUserId());
		notificationService.onOrderPlaced(event);
	}

	@KafkaListener(
			topics = "${kafka.topics.payment-confirmed}",
			groupId = "${spring.kafka.consumer.group-id}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void onPaymentConfirmed(PaymentConfirmedEvent event) {
		log.info("Received PaymentConfirmedEvent: orderId={}, userId={}", event.getOrderId(), event.getUserId());
		notificationService.onPaymentConfirmed(event);
	}

	@KafkaListener(
			topics = "${kafka.topics.payment-failed}",
			groupId = "${spring.kafka.consumer.group-id}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void onPaymentFailed(PaymentFailedEvent event) {
		log.info("Received PaymentFailedEvent: orderId={}, userId={}", event.getOrderId(), event.getUserId());
		notificationService.onPaymentFailed(event);
	}
}
