package com.order_service.kafka;

import com.order_service.dto.event.OrderPlacedEvent;

/**
 * Published within the order transaction; relayed to Kafka after successful commit.
 */
public record OrderPlacedCommittedEvent(OrderPlacedEvent payload) {}
