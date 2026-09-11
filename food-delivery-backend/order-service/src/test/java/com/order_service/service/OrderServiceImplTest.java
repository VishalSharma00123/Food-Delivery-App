package com.order_service.service;

import com.order_service.client.RestaurantCatalogClient;
import com.order_service.client.RestaurantClient;
import com.order_service.dto.MenuItemDto;
import com.order_service.dto.OrderDto;
import com.order_service.dto.OrderItemRequestDto;
import com.order_service.dto.OrderRequestDto;
import com.order_service.entity.Order;
import com.order_service.entity.OrderItem;
import com.order_service.kafka.OrderPlacedCommittedEvent;
import com.order_service.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

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

    @Mock
    private RestaurantCatalogClient restaurantCatalogClient;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequestDto sampleRequest;
    private MenuItemDto sampleMenuItem;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("test@test.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

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

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
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
        verify(applicationEventPublisher, times(1)).publishEvent(any(OrderPlacedCommittedEvent.class));
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
        assertThrows(ResponseStatusException.class, () -> {
            orderService.cancelOrder(500L);
        });
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCancelOrder_OrderNotFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderService.cancelOrder(404L));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateOrder_FlattensMenuAcrossCategories() {
        MenuItemDto drink = MenuItemDto.builder()
                .id(20L)
                .name("Cola")
                .price(new BigDecimal("3.00"))
                .isAvailable(true)
                .build();
        Map<String, List<MenuItemDto>> menu = Map.of(
                "Food", List.of(sampleMenuItem),
                "Drinks", List.of(drink)
        );
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(2L)
                .restaurantId(200L)
                .items(List.of(
                        OrderItemRequestDto.builder().menuItemId(10L).quantity(1).build(),
                        OrderItemRequestDto.builder().menuItemId(20L).quantity(2).build()
                ))
                .build();
        when(restaurantClient.getRestaurantMenu(200L)).thenReturn(menu);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(600L);
            return saved;
        });

        OrderDto result = orderService.createOrder(request);

        assertEquals(600L, result.getId());
        assertEquals(new BigDecimal("21.00"), result.getTotalAmount());
        assertEquals(2, result.getItems().size());
        verify(orderRepository).save(any(Order.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(OrderPlacedCommittedEvent.class));
    }

    @Test
    void testGetOrderById_Success() {
        OrderItem line = OrderItem.builder()
                .id(1L)
                .menuItemId(10L)
                .quantity(2)
                .priceAtPurchase(new BigDecimal("15.00"))
                .order(sampleOrder)
                .build();
        sampleOrder.setItems(List.of(line));
        when(orderRepository.findById(500L)).thenReturn(Optional.of(sampleOrder));

        OrderDto result = orderService.getOrderById(500L);

        assertEquals(500L, result.getId());
        assertEquals(1, result.getItems().size());
        assertEquals(10L, result.getItems().get(0).getMenuItemId());
    }

    @Test
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    void testGetOrdersByUserId_ReturnsMappedList() {
        Order second = Order.builder()
                .id(2L)
                .userId(1L)
                .restaurantId(50L)
                .status("CONFIRMED")
                .totalAmount(BigDecimal.TEN)
                .items(Collections.emptyList())
                .build();
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(sampleOrder, second));

        List<OrderDto> result = orderService.getOrdersByUserId(1L);

        assertEquals(2, result.size());
        assertEquals(500L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void testGetOrdersByRestaurantId_ReturnsMappedList() {
        when(orderRepository.findByRestaurantId(100L)).thenReturn(List.of(sampleOrder));

        List<OrderDto> result = orderService.getOrdersByRestaurantId(100L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getRestaurantId());
    }

    @Test
    void testUpdateOrderStatus_Success() {
        when(orderRepository.findById(500L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto result = orderService.updateOrderStatus(500L, "CONFIRMED");

        assertEquals("CONFIRMED", result.getStatus());
        verify(orderRepository).save(sampleOrder);
    }

    @Test
    void testUpdateOrderStatus_NotFound() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderService.updateOrderStatus(404L, "CONFIRMED"));

        verify(orderRepository, never()).save(any(Order.class));
    }
}
