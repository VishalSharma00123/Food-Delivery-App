package com.order_service.service;

import com.order_service.client.RestaurantClient;
import com.order_service.dto.MenuItemDto;
import com.order_service.dto.OrderDto;
import com.order_service.dto.OrderItemDto;
import com.order_service.dto.OrderRequestDto;
import com.order_service.entity.Order;
import com.order_service.entity.OrderItem;
import com.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantClient restaurantClient;

    @Override
    @Transactional
    public OrderDto createOrder(OrderRequestDto requestDto) {
        // Fetch menu from restaurant service
        Map<String, List<MenuItemDto>> groupedMenu = restaurantClient.getRestaurantMenu(requestDto.getRestaurantId());
        
        // Flatten the menu for easy lookup
        List<MenuItemDto> flatMenu = groupedMenu.values().stream()
                .flatMap(List::stream) // List::stream equals to list -> list.stream()
                .collect(Collectors.toList());
        
        Map<Long, MenuItemDto> menuMap = flatMenu.stream()
                .collect(Collectors.toMap(MenuItemDto::getId, item -> item));

        // Create the Order
        Order order = Order.builder()
                .userId(requestDto.getUserId())
                .restaurantId(requestDto.getRestaurantId())
                .status("PENDING")
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderItem> orderItems = requestDto.getItems().stream().map(reqItem -> {
            MenuItemDto catalogItem = menuMap.get(reqItem.getMenuItemId());
            if (catalogItem == null) {
                throw new RuntimeException("Menu item not found: " + reqItem.getMenuItemId());
            }
            if (!catalogItem.getIsAvailable()) {
                throw new RuntimeException("Menu item is currently out of stock: " + catalogItem.getName());
            }

            BigDecimal itemTotal = catalogItem.getPrice().multiply(BigDecimal.valueOf(reqItem.getQuantity()));
            
            return OrderItem.builder()
                    .order(order)
                    .menuItemId(catalogItem.getId())
                    .quantity(reqItem.getQuantity())
                    .priceAtPurchase(catalogItem.getPrice())
                    .build();
        }).collect(Collectors.toList());

        for (OrderItem item : orderItems) {
            BigDecimal lineTotal = item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        return mapToDto(orderRepository.save(order));
    }

    @Override
    public OrderDto getOrderById(Long id) {
        return mapToDto(orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found")));
    }

    @Override
    public List<OrderDto> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getOrdersByRestaurantId(Long restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDto updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        return mapToDto(orderRepository.save(order));
    }

    @Override
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Cannot cancel an order that is actively being prepared or delivered.");
        }
        
        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    private OrderDto mapToDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream().map(item -> OrderItemDto.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItemId())
                .quantity(item.getQuantity())
                .priceAtPurchase(item.getPriceAtPurchase())
                .build()).collect(Collectors.toList());

        return OrderDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .restaurantId(order.getRestaurantId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
