package com.notification_service.notification_service.service;

import com.notification_service.notification_service.dto.event.OrderPlacedEvent;
import com.notification_service.notification_service.dto.event.PaymentConfirmedEvent;
import com.notification_service.notification_service.dto.event.PaymentFailedEvent;
import com.notification_service.notification_service.dto.event.UserProfileChangedPayload;
import com.notification_service.notification_service.entity.Notification;
import com.notification_service.notification_service.entity.NotificationType;
import com.notification_service.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	private void asAuthenticatedUser(long userId) {
		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken("test", null, List.of());
		auth.setDetails(userId);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	private OrderPlacedEvent orderPlacedEvent;
	private PaymentConfirmedEvent paymentConfirmedEvent;
	private PaymentFailedEvent paymentFailedEvent;

	@BeforeEach
	void setUp() {
		orderPlacedEvent = OrderPlacedEvent.builder()
				.orderId(10L)
				.userId(100L)
				.restaurantId(1L)
				.totalAmount(new BigDecimal("99.50"))
				.paymentMethod("UPI")
				.timestamp(LocalDateTime.now())
				.build();

		paymentConfirmedEvent = PaymentConfirmedEvent.builder()
				.paymentId(50L)
				.orderId(10L)
				.userId(100L)
				.amount(new BigDecimal("99.50"))
				.paymentMethod("UPI")
				.transactionId("TX-1")
				.timestamp(LocalDateTime.now())
				.build();

		paymentFailedEvent = PaymentFailedEvent.builder()
				.paymentId(51L)
				.orderId(11L)
				.userId(101L)
				.amount(new BigDecimal("20.00"))
				.reason("Insufficient funds")
				.timestamp(LocalDateTime.now())
				.build();
	}

	@Test
	void onOrderPlaced_saves_whenNotDuplicate() {
		when(notificationRepository.existsByUserIdAndTypeAndOrderId(100L, NotificationType.ORDER_PLACED, 10L))
				.thenReturn(false);
		when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

		notificationService.onOrderPlaced(orderPlacedEvent);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		Notification saved = captor.getValue();
		assertEquals(100L, saved.getUserId());
		assertEquals(NotificationType.ORDER_PLACED, saved.getType());
		assertEquals(10L, saved.getOrderId());
		assertFalse(saved.isRead());
	}

	@Test
	void onOrderPlaced_skipsSave_whenDuplicate() {
		when(notificationRepository.existsByUserIdAndTypeAndOrderId(100L, NotificationType.ORDER_PLACED, 10L))
				.thenReturn(true);

		notificationService.onOrderPlaced(orderPlacedEvent);

		verify(notificationRepository, never()).save(any());
	}

	@Test
	void onOrderPlaced_doesNothing_whenUserIdNull() {
		orderPlacedEvent.setUserId(null);

		notificationService.onOrderPlaced(orderPlacedEvent);

		verify(notificationRepository, never()).existsByUserIdAndTypeAndOrderId(any(), any(), any());

		/**
		 * 🎯 Simple Understanding
		     * Mockito Code	Meaning
			 * verify(mock)	method WAS called
			 * verify(mock, never())	method was NOT called
			 * any()	match any input
		 * */
	}

	@Test
	void onPaymentConfirmed_saves_whenNotDuplicate() {
		when(notificationRepository.existsByUserIdAndTypeAndOrderId(100L, NotificationType.PAYMENT_CONFIRMED, 10L))
				.thenReturn(false);
		when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

		notificationService.onPaymentConfirmed(paymentConfirmedEvent);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		assertEquals(NotificationType.PAYMENT_CONFIRMED, captor.getValue().getType());
	}

	@Test
	void onUserProfileChanged_saves_whenAuthUserIdPresent() {
		UserProfileChangedPayload payload = UserProfileChangedPayload.builder()
				.type(UserProfileChangedPayload.ChangeType.UPDATED)
				.profileId(5L)
				.authUserId(200L)
				.occurredAt(Instant.parse("2026-01-01T12:00:00Z"))
				.build();
		when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

		notificationService.onUserProfileChanged(payload);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		assertEquals(200L, captor.getValue().getUserId());
		assertEquals(NotificationType.USER_PROFILE_CHANGED, captor.getValue().getType());
		assertEquals(5L, captor.getValue().getProfileId());
	}

	@Test
	void onUserProfileChanged_skipsSave_whenAuthUserIdNull() {
		UserProfileChangedPayload payload = UserProfileChangedPayload.builder()
				.type(UserProfileChangedPayload.ChangeType.CREATED)
				.profileId(1L)
				.authUserId(null)
				.occurredAt(Instant.now())
				.build();

		notificationService.onUserProfileChanged(payload);

		verify(notificationRepository, never()).save(any());
	}

	@Test
	void onPaymentFailed_saves_whenNotDuplicate() {
		when(notificationRepository.existsByUserIdAndTypeAndOrderId(101L, NotificationType.PAYMENT_FAILED, 11L))
				.thenReturn(false);
		when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

		notificationService.onPaymentFailed(paymentFailedEvent);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		assertEquals(NotificationType.PAYMENT_FAILED, captor.getValue().getType());
	}

	@Test
	void markAsRead_returnsTrue_andUpdates() {
		asAuthenticatedUser(100L);
		Notification n = Notification.builder()
				.id(1L)
				.userId(100L)
				.type(NotificationType.ORDER_PLACED)
				.title("t")
				.body("b")
				.orderId(10L)
				.read(false)
				.createdAt(LocalDateTime.now())
				.build();
		when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
		when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

		boolean result = notificationService.markAsRead(100L, 1L);

		assertTrue(result);
		assertTrue(n.isRead());
		verify(notificationRepository).save(n);
	}

	@Test
	void markAsRead_returnsFalse_whenWrongUser() {
		asAuthenticatedUser(999L);
		Notification n = Notification.builder()
				.id(1L)
				.userId(100L)
				.type(NotificationType.ORDER_PLACED)
				.title("t")
				.body("b")
				.orderId(10L)
				.read(false)
				.createdAt(LocalDateTime.now())
				.build();
		when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

		boolean result = notificationService.markAsRead(999L, 1L);

		assertFalse(result);
		verify(notificationRepository, never()).save(any());
	}

	@Test
	void listForUser_unreadOnly_usesUnreadQuery() {
		asAuthenticatedUser(100L);
		when(notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(100L, false))
				.thenReturn(Collections.emptyList());

		notificationService.listForUser(100L, true);

		verify(notificationRepository).findByUserIdAndReadOrderByCreatedAtDesc(100L, false);
	}

	@Test
	void listForUser_all_usesFullQuery() {
		asAuthenticatedUser(100L);
		when(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(List.of());

		notificationService.listForUser(100L, false);

		verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(100L);
	}
}
