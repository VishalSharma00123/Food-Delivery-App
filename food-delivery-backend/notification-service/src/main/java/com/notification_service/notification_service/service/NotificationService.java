package com.notification_service.notification_service.service;

import com.notification_service.notification_service.dto.NotificationDto;
import com.notification_service.notification_service.dto.event.OrderPlacedEvent;
import com.notification_service.notification_service.dto.event.PaymentConfirmedEvent;
import com.notification_service.notification_service.dto.event.PaymentFailedEvent;

import java.util.List;

public interface NotificationService {

	void onOrderPlaced(OrderPlacedEvent event);

	void onPaymentConfirmed(PaymentConfirmedEvent event);

	void onPaymentFailed(PaymentFailedEvent event);

	List<NotificationDto> listForUser(Long userId, Boolean unreadOnly);

	boolean markAsRead(Long userId, Long notificationId);
}
