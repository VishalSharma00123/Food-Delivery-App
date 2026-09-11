package com.payment_service.payment_service.service;

import com.fooddelivery.rbac.RbacSupport;
import com.fooddelivery.rbac.SecurityRoleUtils;
import com.payment_service.payment_service.dto.PaymentDto;
import com.payment_service.payment_service.dto.RazorpayCheckoutResponse;
import com.payment_service.payment_service.dto.RazorpayInitiateRequest;
import com.payment_service.payment_service.dto.RazorpayVerifyRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
    private final RazorpayGatewayService razorpayGatewayService;
    private final PaymentInsertService paymentInsertService;

    @Override
    @Transactional
    public PaymentDto processPayment(OrderPlacedEvent event) {
        PaymentMethod method = resolveMethod(event.getPaymentMethod());
        Payment payment = findOrCreatePendingPayment(
                event.getOrderId(),
                event.getUserId(),
                event.getTotalAmount(),
                method
        );

        // Already finished (e.g. concurrent initiate/verify) — do not reprocess.
        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            log.warn("Payment already terminal for orderId={}, status={}",
                    event.getOrderId(), payment.getStatus());
            return mapToDto(payment);
        }

        // Online methods (CARD / UPI) wait for Razorpay Checkout + verify.
        if (isOnlineMethod(method)) {
            if (payment.getStatus() == PaymentStatus.FAILED) {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setFailureReason(null);
                payment.setRazorpayOrderId(null);
            }
            try {
                if (payment.getRazorpayOrderId() == null || payment.getRazorpayOrderId().isBlank()) {
                    String razorpayOrderId = razorpayGatewayService.createOrder(
                            event.getOrderId(), event.getTotalAmount());
                    payment.setRazorpayOrderId(razorpayOrderId);
                    payment.setStatus(PaymentStatus.PENDING);
                    payment = paymentRepository.save(payment);
                }
                log.info("Payment PENDING (Razorpay): orderId={}, razorpayOrderId={}",
                        event.getOrderId(), payment.getRazorpayOrderId());
            } catch (Exception ex) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(ex.getMessage());
                payment = paymentRepository.save(payment);
                publishFailed(payment, ex.getMessage());
            }
            return mapToDto(payment);
        }

        // COD (and any offline strategy) is confirmed immediately.
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return mapToDto(payment);
        }
        try {
            PaymentStrategy strategy = strategyFactory.getStrategy(method.name());
            String transactionId = strategy.pay(event.getTotalAmount());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(transactionId);
            payment = paymentRepository.save(payment);
            publishConfirmed(payment, transactionId);
            log.info("Payment SUCCESS: orderId={}, txnId={}", event.getOrderId(), transactionId);
        } catch (PaymentProcessingException | IllegalArgumentException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            payment = paymentRepository.save(payment);
            publishFailed(payment, ex.getMessage());
            log.error("Payment FAILED: orderId={}, reason={}", event.getOrderId(), ex.getMessage());
        }

        return mapToDto(payment);
    }

    @Override
    @Transactional
    public RazorpayCheckoutResponse createRazorpayCheckout(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment for this order"));
        return toCheckoutResponse(ensureRazorpayOrder(payment));
    }

    @Override
    @Transactional
    public RazorpayCheckoutResponse initiateRazorpayCheckout(RazorpayInitiateRequest request) {
        Long uid = RbacSupport.requireUserId();
        PaymentMethod method = resolveMethod(request.getPaymentMethod());
        if (!isOnlineMethod(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CARD/UPI can use Razorpay checkout");
        }

        Payment payment = findOrCreatePendingPayment(
                request.getOrderId(),
                uid,
                request.getAmount(),
                method
        );

        if (!SecurityRoleUtils.isAdmin()
                && (payment.getUserId() == null || !payment.getUserId().equals(uid))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to pay for this order");
        }

        // Allow retry after a previous Razorpay create failure.
        if (payment.getStatus() == PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setFailureReason(null);
            payment.setRazorpayOrderId(null);
            payment = paymentRepository.save(payment);
        }

        return toCheckoutResponse(ensureRazorpayOrder(payment));
    }

    /**
     * Kafka {@code order.placed} and HTTP initiate often race for the same orderId.
     * Insert runs in a nested transaction so a unique violation does not break this session.
     */
    private Payment findOrCreatePendingPayment(
            Long orderId,
            Long userId,
            BigDecimal amount,
            PaymentMethod method) {
        Payment existing = paymentRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            return existing;
        }

        try {
            Payment saved = paymentInsertService.insertPending(orderId, userId, amount, method);
            log.info("Created PENDING payment for orderId={}", orderId);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.info("Payment row already created concurrently for orderId={}, reusing it", orderId);
            return paymentRepository.findByOrderId(orderId).orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional
    public PaymentDto verifyRazorpayPayment(RazorpayVerifyRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment for this order"));
        assertPaymentReadable(payment);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return mapToDto(payment);
        }
        if (payment.getRazorpayOrderId() == null
                || !payment.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order mismatch");
        }

        boolean valid = razorpayGatewayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid Razorpay signature");
            payment = paymentRepository.save(payment);
            publishFailed(payment, "Invalid Razorpay signature");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setFailureReason(null);
        payment = paymentRepository.save(payment);
        publishConfirmed(payment, request.getRazorpayPaymentId());
        log.info("Razorpay payment verified: orderId={}, paymentId={}",
                payment.getOrderId(), request.getRazorpayPaymentId());
        return mapToDto(payment);
    }

    @Override
    public PaymentDto getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        assertPaymentReadable(payment);
        return mapToDto(payment);
    }

    @Override
    public PaymentDto getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment for this order"));
        assertPaymentReadable(payment);
        return mapToDto(payment);
    }

    @Override
    public List<PaymentDto> getPaymentsByUserId(Long userId) {
        RbacSupport.assertSelfOrAdmin(userId, "payments");
        return paymentRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentDto refundPayment(Long paymentId) {
        if (!SecurityRoleUtils.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can refund");
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only successful payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);
        log.info("Payment refunded: paymentId={}, orderId={}", paymentId, payment.getOrderId());
        return mapToDto(payment);
    }

    private boolean isOnlineMethod(PaymentMethod method) {
        return method == PaymentMethod.CARD || method == PaymentMethod.UPI;
    }

    private Payment ensureRazorpayOrder(Payment payment) {
        if (!isOnlineMethod(payment.getPaymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not an online Razorpay payment");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment already completed");
        }
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not payable: " + payment.getStatus());
        }
        if (payment.getRazorpayOrderId() == null || payment.getRazorpayOrderId().isBlank()) {
            String razorpayOrderId = razorpayGatewayService.createOrder(payment.getOrderId(), payment.getAmount());
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setStatus(PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);
        }
        return payment;
    }

    private RazorpayCheckoutResponse toCheckoutResponse(Payment payment) {
        return RazorpayCheckoutResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .keyId(razorpayGatewayService.getKeyId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .amountPaise(razorpayGatewayService.toPaise(payment.getAmount()))
                .currency(razorpayGatewayService.getCurrency())
                .status(payment.getStatus().name())
                .build();
    }

    private void publishConfirmed(Payment payment, String transactionId) {
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
    }

    private void publishFailed(Payment payment, String reason) {
        PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
        paymentEventProducer.publishPaymentFailed(failedEvent);
    }

    private void assertPaymentReadable(Payment payment) {
        if (SecurityRoleUtils.isAdmin()) {
            return;
        }
        Long uid = RbacSupport.requireUserId();
        if (!payment.getUserId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this payment");
        }
    }

    private PaymentMethod resolveMethod(String method) {
        if (method == null || method.isBlank()) {
            return PaymentMethod.COD;
        }
        try {
            return PaymentMethod.valueOf(method.trim().toUpperCase());
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
                .razorpayOrderId(payment.getRazorpayOrderId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
