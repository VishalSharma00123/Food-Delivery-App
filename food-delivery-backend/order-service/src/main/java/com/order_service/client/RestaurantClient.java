package com.order_service.client;

import com.order_service.dto.MenuItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "restaurant-service", url = "${restaurant-service.url}")
public interface RestaurantClient {

    @GetMapping("/api/public/restaurants/{restaurantId}/menu")
    Map<String, List<MenuItemDto>> getRestaurantMenu(@PathVariable("restaurantId") Long restaurantId);
}
