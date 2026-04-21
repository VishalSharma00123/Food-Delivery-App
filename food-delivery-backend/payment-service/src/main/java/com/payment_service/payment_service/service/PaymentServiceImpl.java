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
import com.payment_service.payment_service.strategy.PaymentProcessingException;
import com.payment_service.payment_service.strategy.PaymentStrategy;
import com.payment_service.payment_service.strategy.PaymentStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStrategyFactory strategyFactory;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional
    public PaymentDto processPayment(OrderPlacedEvent event) {
        // Idempotency check — prevent double-charging the same order
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Duplicate payment event ignored for orderId={}", event.getOrderId());
            return mapToDto(paymentRepository.findByOrderId(event.getOrderId()).orElseThrow());
        }

        // Create a PENDING payment record
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .paymentMethod(resolveMethod(event.getPaymentMethod()))
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        // Process payment via the appropriate strategy
        try {
            PaymentStrategy strategy = strategyFactory.getStrategy(event.getPaymentMethod());
            String transactionId = strategy.pay(event.getTotalAmount());

            // Mark as SUCCESS
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(transactionId);
            payment = paymentRepository.save(payment);
            log.info("Payment SUCCESS: orderId={}, txnId={}", event.getOrderId(), transactionId);

            // Publish payment.confirmed event
            PaymentConfirmedEvent confirmedEvent = PaymentConfirmedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .paymentMethod(payment.getPaymentMethod().name())
                    .transactionId(transactionId)
                    .timestamp(LocalDateTime.now())
                    .build();
            paymentEventProducer.publishPaymentConfirmed(confirmedEvent);

        } catch (PaymentProcessingException | IllegalArgumentException ex) {
            // Mark as FAILED
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            payment = paymentRepository.save(payment);
            log.error("Payment FAILED: orderId={}, reason={}", event.getOrderId(), ex.getMessage());

            // Publish payment.failed event
            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .reason(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            paymentEventProducer.publishPaymentFailed(failedEvent);
        }

        return mapToDto(payment);
    }

    @Override
    public PaymentDto getPaymentById(Long paymentId) {
        return mapToDto(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId)));
    }

    @Override
    public PaymentDto getPaymentByOrderId(Long orderId) {
        return mapToDto(paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("No payment found for orderId: " + orderId)));
    }

    @Override
    public List<PaymentDto> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentDto refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);
        log.info("Payment refunded: paymentId={}, orderId={}", paymentId, payment.getOrderId());
        return mapToDto(payment);
    }

    private PaymentMethod resolveMethod(String method) {
        try {
            return PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown payment method '{}', defaulting to COD", method);
            return PaymentMethod.COD;
        }
    }

    private PaymentDto mapToDto(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
