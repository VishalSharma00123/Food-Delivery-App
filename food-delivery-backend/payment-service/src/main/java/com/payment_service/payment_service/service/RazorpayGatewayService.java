package com.payment_service.payment_service.service;

import com.payment_service.payment_service.config.RazorpayProperties;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayGatewayService {

    private final RazorpayClient razorpayClient;
    private final RazorpayProperties properties;

    public String createOrder(Long appOrderId, BigDecimal amount) {
        try {
            long amountPaise = toPaise(amount);
            JSONObject request = new JSONObject();
            request.put("amount", amountPaise);
            request.put("currency", properties.getCurrency());
            request.put("receipt", "order_" + appOrderId);
            request.put("payment_capture", 1);

            Order order = razorpayClient.orders.create(request);
            String razorpayOrderId = order.get("id");
            log.info("Created Razorpay order {} for app orderId={} amountPaise={}",
                    razorpayOrderId, appOrderId, amountPaise);
            return razorpayOrderId;
        } catch (RazorpayException ex) {
            log.error("Razorpay order create failed for orderId={}: {}", appOrderId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create Razorpay order");
        }
    }

    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(attributes, properties.getKeySecret());
        } catch (RazorpayException ex) {
            log.warn("Razorpay signature verification error: {}", ex.getMessage());
            return false;
        }
    }

    public long toPaise(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment amount");
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public String getKeyId() {
        return properties.getKeyId();
    }

    public String getCurrency() {
        return properties.getCurrency();
    }
}
