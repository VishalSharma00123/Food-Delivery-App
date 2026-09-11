package com.payment_service.payment_service.controller;

import com.payment_service.payment_service.dto.PaymentDto;
import com.payment_service.payment_service.dto.RazorpayCheckoutResponse;
import com.payment_service.payment_service.dto.RazorpayInitiateRequest;
import com.payment_service.payment_service.dto.RazorpayVerifyRequest;
import com.payment_service.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * GET /api/payments/razorpay/checkout/{orderId}
     * Returns Razorpay Checkout.js parameters for an online payment.
     */
    @GetMapping("/razorpay/checkout/{orderId}")
    public ResponseEntity<RazorpayCheckoutResponse> createRazorpayCheckout(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.createRazorpayCheckout(orderId));
    }

    /**
     * POST /api/payments/razorpay/initiate
     * Starts Razorpay checkout even when Kafka has not created the payment row yet.
     */
    @PostMapping("/razorpay/initiate")
    public ResponseEntity<RazorpayCheckoutResponse> initiateRazorpayCheckout(
            @Valid @RequestBody RazorpayInitiateRequest request) {
        return ResponseEntity.ok(paymentService.initiateRazorpayCheckout(request));
    }

    /**
     * POST /api/payments/razorpay/verify
     * Verifies Razorpay payment signature and confirms the order payment.
     */
    @PostMapping("/razorpay/verify")
    public ResponseEntity<PaymentDto> verifyRazorpayPayment(@Valid @RequestBody RazorpayVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verifyRazorpayPayment(request));
    }

    /**
     * GET /api/payments/{paymentId}
     * Fetch payment details by its own ID.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getPaymentById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    /**
     * GET /api/payments/order/{orderId}
     * Fetch the payment record associated with a specific order.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDto> getPaymentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    /**
     * GET /api/payments/user/{userId}
     * Fetch all payment records for a user (payment history).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }

    /**
     * POST /api/payments/{paymentId}/refund
     * Initiate a refund for a successful payment.
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentDto> refundPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }
}
