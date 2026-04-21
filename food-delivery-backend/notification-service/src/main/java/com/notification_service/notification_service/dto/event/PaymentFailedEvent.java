package com.notification_service.notification_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
	private Long paymentId;
	private Long orderId;
	private Long userId;
	private BigDecimal amount;
	private String reason;
	private LocalDateTime timestamp;
}
