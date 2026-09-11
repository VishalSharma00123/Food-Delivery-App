package com.order_service.service;

import com.fooddelivery.rbac.RbacSupport;
import com.fooddelivery.rbac.SecurityRoleUtils;
import com.order_service.client.RestaurantCatalogClient;
import com.order_service.client.RestaurantClient;
import com.order_service.dto.MenuItemDto;
import com.order_service.dto.OrderDto;
import com.order_service.dto.OrderItemDto;
import com.order_service.dto.OrderRequestDto;
import com.order_service.dto.event.OrderPlacedEvent;
import com.order_service.entity.Order;
import com.order_service.entity.OrderItem;
import com.order_service.kafka.OrderPlacedCommittedEvent;
import com.order_service.repository.OrderRepository;
import com.order_service.dto.RestaurantSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantClient restaurantClient;
    private final RestaurantCatalogClient restaurantCatalogClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public OrderDto createOrder(OrderRequestDto requestDto) {
        if (!SecurityRoleUtils.isAdmin()) {
            Long uid = RbacSupport.requireUserId();
            if (!uid.equals(requestDto.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only create orders for yourself");
            }
        }
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

        Order saved = orderRepository.save(order);
        applicationEventPublisher.publishEvent(new OrderPlacedCommittedEvent(
                OrderPlacedEvent.builder()
                        .orderId(saved.getId())
                        .userId(saved.getUserId())
                        .restaurantId(saved.getRestaurantId())
                        .totalAmount(saved.getTotalAmount())
                        .paymentMethod(requestDto.getPaymentMethod())
                        .timestamp(LocalDateTime.now())
                        .build()));
        return mapToDto(saved);
    }

    @Override
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        assertCanViewOrder(order);
        return mapToDto(order);
    }

    @Override
    public List<OrderDto> getOrdersByUserId(Long userId) {
        RbacSupport.assertSelfOrAdmin(userId, "orders");
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getOrdersByRestaurantId(Long restaurantId) {
        assertCanViewRestaurantOrders(restaurantId);
        return orderRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDto updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        assertCanUpdateOrderStatus(order);
        order.setStatus(status);
        return mapToDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void updateOrderStatusInternal(Long orderId, String status) {
        // Used by Kafka consumers — no security context available, skip auth check
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
    }

    @Override
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        assertCanCancelOrder(order);

        if (!"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel an order that is actively being prepared or delivered.");
        }
        
        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    private void assertCanViewOrder(Order order) {
        if (SecurityRoleUtils.isAdmin()) {
            return;
        }
        Long uid = RbacSupport.requireUserId();
        if (order.getUserId().equals(uid)) {
            return;
        }
        if (SecurityRoleUtils.isRestaurantOwner() && isRestaurantOwner(order.getRestaurantId(), uid)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this order");
    }

    private void assertCanViewRestaurantOrders(Long restaurantId) {
        if (SecurityRoleUtils.isAdmin()) {
            return;
        }
        if (SecurityRoleUtils.isRestaurantOwner()) {
            Long uid = RbacSupport.requireUserId();
            if (isRestaurantOwner(restaurantId, uid)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to list orders for this restaurant");
    }

    private void assertCanUpdateOrderStatus(Order order) {
        if (SecurityRoleUtils.isAdmin()) {
            return;
        }
        Long uid = RbacSupport.requireUserId();
        if (SecurityRoleUtils.isRestaurantOwner() && isRestaurantOwner(order.getRestaurantId(), uid)) {
            return;
        }
        if (SecurityRoleUtils.isDeliveryPartner()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update order status");
    }

    private void assertCanCancelOrder(Order order) {
        if (SecurityRoleUtils.isAdmin()) {
            return;
        }
        Long uid = RbacSupport.requireUserId();
        if (order.getUserId().equals(uid)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to cancel this order");
    }

    private boolean isRestaurantOwner(Long restaurantId, Long userId) {
        try {
            RestaurantSummaryDto dto = restaurantCatalogClient.getRestaurant(restaurantId);
            return dto.getOwnerId() != null && dto.getOwnerId().equals(userId);
        } catch (Exception ex) {
            return false;
        }
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
