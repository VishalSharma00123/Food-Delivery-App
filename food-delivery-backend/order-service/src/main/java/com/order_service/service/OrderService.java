package com.order_service.service;

import com.order_service.dto.OrderDto;
import com.order_service.dto.OrderRequestDto;

import java.util.List;

public interface OrderService {
    OrderDto createOrder(OrderRequestDto requestDto);
    OrderDto getOrderById(Long id);
    List<OrderDto> getOrdersByUserId(Long userId);
    List<OrderDto> getOrdersByRestaurantId(Long restaurantId);
    OrderDto updateOrderStatus(Long orderId, String status);
    void cancelOrder(Long orderId);
}
