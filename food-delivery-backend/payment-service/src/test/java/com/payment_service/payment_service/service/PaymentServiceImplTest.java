package com.payment_service.payment_service.service;

import com.payment_service.payment_service.dto.PaymentDto;
import com.payment_service.payment_service.dto.event.OrderPlacedEvent;
import com.payment_service.payment_service.dto.event.PaymentConfirmedEvent;
import com.payment_service.payment_service.dto.event.PaymentFailedEvent;
import com.payment_service.payment_service.entity.Payment;
import com.payment_service.payment_service.entity.enums.PaymentMethod;
import com.payment_service.payment_service.entity.enums.PaymentStatus;
import com.payment_service.payment_service.kafka.PaymentEventProducer;
import com.payment_service.payment_service.repository.PaymentRepository;
import com.payment_service.payment_service.strategy.PaymentStrategy;
import com.payment_service.payment_service.strategy.PaymentStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStrategyFactory strategyFactory;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private OrderPlacedEvent orderPlacedEvent;
    private Payment payment;

    @BeforeEach
    void setUp() {
        orderPlacedEvent = OrderPlacedEvent.builder()
                .orderId(1L)
                .userId(101L)
                .totalAmount(new BigDecimal("500.00"))
                .paymentMethod("UPI")
                .timestamp(LocalDateTime.now())
                .build();

        payment = Payment.builder()
                .id(1L)
                .orderId(1L)
                .userId(101L)
                .amount(new BigDecimal("500.00"))
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void processPayment_ShouldSucceed_WhenIdempotentAndStrategySucceeds() {
        // Arrange
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(strategyFactory.getStrategy("UPI")).thenReturn(paymentStrategy);
        when(paymentStrategy.pay(any(BigDecimal.class))).thenReturn("TXN-123");

        // Act
        PaymentDto result = paymentService.processPayment(orderPlacedEvent);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("TXN-123", result.getTransactionId());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(paymentEventProducer, times(1)).publishPaymentConfirmed(any(PaymentConfirmedEvent.class));
    }

    @Test
    void processPayment_ShouldReturnExisting_WhenDuplicateOrderIdReceived() {
        // Arrange
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        // Act
        PaymentDto result = paymentService.processPayment(orderPlacedEvent);

        // Assert
        assertNotNull(result);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(strategyFactory, never()).getStrategy(anyString());
    }

    @Test
    void processPayment_ShouldHandleFailure_WhenStrategyThrowsException() {
        // Arrange
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(strategyFactory.getStrategy("UPI")).thenThrow(new IllegalArgumentException("Unsupported method"));

        // Act
        PaymentDto result = paymentService.processPayment(orderPlacedEvent);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(paymentEventProducer, times(1)).publishPaymentFailed(any(PaymentFailedEvent.class));
    }

    @Test
    void refundPayment_ShouldSucceed_WhenPaymentIsSuccessful() {
        // Arrange
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        PaymentDto result = paymentService.refundPayment(1L);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
    }

    @Test
    void refundPayment_ShouldThrowException_WhenPaymentIsNotSuccessful() {
        // Arrange
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> paymentService.refundPayment(1L));
    }
}
