package com.notification_service.notification_service.service;

import com.notification_service.notification_service.dto.event.OrderPlacedEvent;
import com.notification_service.notification_service.dto.event.PaymentConfirmedEvent;
import com.notification_service.notification_service.dto.event.PaymentFailedEvent;
import com.notification_service.notification_service.entity.Notification;
import com.notification_service.notification_service.entity.NotificationType;
import com.notification_service.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
		when(notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(100L, false))
				.thenReturn(Collections.emptyList());

		notificationService.listForUser(100L, true);

		verify(notificationRepository).findByUserIdAndReadOrderByCreatedAtDesc(100L, false);
	}

	@Test
	void listForUser_all_usesFullQuery() {
		when(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(List.of());

		notificationService.listForUser(100L, false);

		verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(100L);
	}
}
