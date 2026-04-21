package com.payment_service.payment_service.service;

import com.payment_service.payment_service.dto.PaymentDto;
import com.payment_service.payment_service.dto.event.OrderPlacedEvent;

import java.util.List;

public interface PaymentService {
    PaymentDto processPayment(OrderPlacedEvent event);
    PaymentDto getPaymentById(Long paymentId);
    PaymentDto getPaymentByOrderId(Long orderId);
    List<PaymentDto> getPaymentsByUserId(Long userId);
    PaymentDto refundPayment(Long paymentId);
}
