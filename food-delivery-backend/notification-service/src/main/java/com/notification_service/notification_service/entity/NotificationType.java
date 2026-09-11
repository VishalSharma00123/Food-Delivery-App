package com.notification_service.notification_service.entity;

public enum NotificationType {
	ORDER_PLACED,
	// internally it created,   public static final Day ORDER_PLACED = new NotificationType();
	PAYMENT_CONFIRMED,
	PAYMENT_FAILED,
	USER_PROFILE_CHANGED
}
