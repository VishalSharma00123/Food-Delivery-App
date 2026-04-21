package com.order_service.service;

import com.order_service.client.RestaurantClient;
import com.order_service.dto.MenuItemDto;
import com.order_service.dto.OrderDto;
import com.order_service.dto.OrderItemRequestDto;
import com.order_service.dto.OrderRequestDto;
import com.order_service.entity.Order;
import com.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestaurantClient restaurantClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequestDto sampleRequest;
    private MenuItemDto sampleMenuItem;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleRequest = OrderRequestDto.builder()
                .userId(1L)
                .restaurantId(100L)
                .items(List.of(
                        OrderItemRequestDto.builder().menuItemId(10L).quantity(2).build()
                ))
                .build();

        sampleMenuItem = MenuItemDto.builder()
                .id(10L)
                .name("Pizza")
                .price(new BigDecimal("15.00"))
                .isAvailable(true)
                .build();

        sampleOrder = Order.builder()
                .id(500L)
                .userId(1L)
                .restaurantId(100L)
                .status("PENDING")
                .totalAmount(new BigDecimal("30.00"))
                .items(Collections.emptyList()) 
                .build();
    }


    @Test
    void testCreateOrder_Success() {
        // Arrange
        Map<String, List<MenuItemDto>> mockMenu = Map.of("Main", List.of(sampleMenuItem));
        when(restaurantClient.getRestaurantMenu(anyLong())).thenReturn(mockMenu);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(500L);
            return savedOrder;
        });

        // Act
        OrderDto result = orderService.createOrder(sampleRequest);

        // Assert
        assertNotNull(result);
        assertEquals(500L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(new BigDecimal("30.00"), result.getTotalAmount()); // 15.00 * 2

        verify(restaurantClient, times(1)).getRestaurantMenu(100L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_ItemNotFound() {
        // Arrange
        Map<String, List<MenuItemDto>> mockMenu = Map.of("Main", List.of(
                MenuItemDto.builder().id(99L).price(new BigDecimal("10")).isAvailable(true).build()
        )); // Does not contain ID 10L
        
        when(restaurantClient.getRestaurantMenu(anyLong())).thenReturn(mockMenu);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(sampleRequest);
        });

        assertEquals("Menu item not found: 10", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateOrder_ItemOutOfStock() {
        // Arrange
        sampleMenuItem.setIsAvailable(false); // Out of stock
        Map<String, List<MenuItemDto>> mockMenu = Map.of("Main", List.of(sampleMenuItem));
        when(restaurantClient.getRestaurantMenu(anyLong())).thenReturn(mockMenu);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(sampleRequest);
        });

        assertTrue(exception.getMessage().contains("Menu item is currently out of stock"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCancelOrder_Success() {
        // Arrange
        when(orderRepository.findById(500L)).thenReturn(Optional.of(sampleOrder));

        // Act
        orderService.cancelOrder(500L);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals("CANCELLED", orderCaptor.getValue().getStatus());
    }

    @Test
    void testCancelOrder_FailsWhenNotPending() {
        // Arrange
        sampleOrder.setStatus("PREPARING");
        when(orderRepository.findById(500L)).thenReturn(Optional.of(sampleOrder));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.cancelOrder(500L));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
