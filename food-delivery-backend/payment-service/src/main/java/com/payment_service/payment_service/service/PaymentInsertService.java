package com.payment_service.payment_service.service;

import com.payment_service.payment_service.entity.Payment;
import com.payment_service.payment_service.entity.enums.PaymentMethod;
import com.payment_service.payment_service.entity.enums.PaymentStatus;
import com.payment_service.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Inserts in a new transaction so a unique-constraint race can roll back
 * without poisoning the caller's Hibernate session.
 */
@Service
@RequiredArgsConstructor
public class PaymentInsertService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment insertPending(Long orderId, Long userId, BigDecimal amount, PaymentMethod method) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .paymentMethod(method)
                .status(PaymentStatus.PENDING)
                .build();
        return paymentRepository.saveAndFlush(payment);
    }
}
