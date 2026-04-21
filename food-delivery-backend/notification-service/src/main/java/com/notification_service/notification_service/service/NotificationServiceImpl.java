package com.notification_service.notification_service.service;

import com.notification_service.notification_service.dto.NotificationDto;
import com.notification_service.notification_service.dto.event.OrderPlacedEvent;
import com.notification_service.notification_service.dto.event.PaymentConfirmedEvent;
import com.notification_service.notification_service.dto.event.PaymentFailedEvent;
import com.notification_service.notification_service.entity.Notification;
import com.notification_service.notification_service.entity.NotificationType;
import com.notification_service.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;

	@Override
	@Transactional
	public void onOrderPlaced(OrderPlacedEvent event) {
		if (event.getUserId() == null || event.getOrderId() == null) {
			return;
		}
		if (notificationRepository.existsByUserIdAndTypeAndOrderId(
				event.getUserId(), NotificationType.ORDER_PLACED, event.getOrderId())) {
			return;
		}
		String amountStr = event.getTotalAmount() != null ? event.getTotalAmount().toPlainString() : "?";
		Notification n = Notification.builder()
				.userId(event.getUserId())
				.type(NotificationType.ORDER_PLACED)
				.title("Order placed")
				.body("Your order #" + event.getOrderId() + " was placed. Amount: " + amountStr + ".")
				.orderId(event.getOrderId())
				.read(false)
				.build();
		notificationRepository.save(n);
	}

	@Override
	@Transactional
	public void onPaymentConfirmed(PaymentConfirmedEvent event) {
		if (event.getUserId() == null || event.getOrderId() == null) {
			return;
		}

		/// If already payment is confirmed, no need to confirm it again
		if (notificationRepository.existsByUserIdAndTypeAndOrderId(
				event.getUserId(), NotificationType.PAYMENT_CONFIRMED, event.getOrderId())) {
			return;
		}
		String txn = event.getTransactionId() != null ? event.getTransactionId() : "n/a";
		Notification n = Notification.builder()
				.userId(event.getUserId())
				.type(NotificationType.PAYMENT_CONFIRMED)
				.title("Payment successful")
				.body("Payment for order #" + event.getOrderId() + " completed. Transaction: " + txn + ".")
				.orderId(event.getOrderId())
				.read(false)
				.build();
		notificationRepository.save(n);
	}

	@Override
	@Transactional
	public void onPaymentFailed(PaymentFailedEvent event) {
		if (event.getUserId() == null || event.getOrderId() == null) {
			return;
		}
		if (notificationRepository.existsByUserIdAndTypeAndOrderId(
				event.getUserId(), NotificationType.PAYMENT_FAILED, event.getOrderId())) {
			return;
		}
		String reason = event.getReason() != null ? event.getReason() : "Unknown error";
		Notification n = Notification.builder()
				.userId(event.getUserId())
				.type(NotificationType.PAYMENT_FAILED)
				.title("Payment failed")
				.body("Payment for order #" + event.getOrderId() + " failed: " + reason + ".")
				.orderId(event.getOrderId())
				.read(false)
				.build();
		notificationRepository.save(n);
	}

	@Override
	@Transactional(readOnly = true)
	public List<NotificationDto> listForUser(Long userId, Boolean unreadOnly) {
		List<Notification> list;
		if (Boolean.TRUE.equals(unreadOnly)) {
			list = notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false);
		} else {
			list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
		}
		return list.stream().map(this::toDto).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public boolean markAsRead(Long userId, Long notificationId) {
		return notificationRepository.findById(notificationId)
				.filter(n -> n.getUserId().equals(userId))
				.map(n -> {
					n.setRead(true);
					notificationRepository.save(n);
					return true;
				})
				.orElse(false);
	}

	private NotificationDto toDto(Notification n) {
		return NotificationDto.builder()
				.id(n.getId())
				.userId(n.getUserId())
				.type(n.getType())
				.title(n.getTitle())
				.body(n.getBody())
				.orderId(n.getOrderId())
				.read(n.isRead())
				.createdAt(n.getCreatedAt())
				.build();
	}
}
