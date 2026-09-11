package com.payment_service.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayCheckoutResponse {
    private Long paymentId;
    private Long orderId;
    private String keyId;
    private String razorpayOrderId;
    private Long amountPaise;
    private String currency;
    private String status;
}
