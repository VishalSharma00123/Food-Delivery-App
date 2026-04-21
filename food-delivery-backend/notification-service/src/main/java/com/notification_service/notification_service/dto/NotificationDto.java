package com.notification_service.notification_service.dto;

import com.notification_service.notification_service.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
	private Long id;
	private Long userId;
	private NotificationType type;
	private String title;
	private String body;
	private Long orderId;
	private boolean read;
	private LocalDateTime createdAt;
}
