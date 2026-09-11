package com.payment_service.payment_service.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Bean
    @ConditionalOnProperty(prefix = "razorpay", name = "key-id")
    public RazorpayClient razorpayClient(RazorpayProperties properties) throws RazorpayException {
        if (properties.getKeySecret() == null || properties.getKeySecret().isBlank()) {
            throw new IllegalStateException(
                    "Razorpay secret missing. Set razorpay.key-secret or RAZORPAY_KEY_SECRET."
            );
        }
        return new RazorpayClient(properties.getKeyId(), properties.getKeySecret());
    }
}
