package com.notification_service.notification_service.controller;

import com.notification_service.notification_service.dto.NotificationDto;
import com.notification_service.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<NotificationDto>> listForUser(
			@PathVariable Long userId,
			@RequestParam(required = false) Boolean unreadOnly) {
		return ResponseEntity.ok(notificationService.listForUser(userId, unreadOnly));
	}

	@PatchMapping("/user/{userId}/{notificationId}/read")
	public ResponseEntity<Void> markAsRead(
			@PathVariable Long userId,
			@PathVariable Long notificationId) {
		boolean updated = notificationService.markAsRead(userId, notificationId);
		if (!updated) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}
}
