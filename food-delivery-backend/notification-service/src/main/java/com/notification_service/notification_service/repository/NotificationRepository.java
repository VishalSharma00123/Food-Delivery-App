package com.notification_service.notification_service.repository;

import com.notification_service.notification_service.entity.Notification;
import com.notification_service.notification_service.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

	List<Notification> findByUserIdAndReadOrderByCreatedAtDesc(Long userId, boolean read);

	boolean existsByUserIdAndTypeAndOrderId(Long userId, NotificationType type, Long orderId);
}
