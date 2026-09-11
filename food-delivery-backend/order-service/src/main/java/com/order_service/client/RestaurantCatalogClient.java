package com.order_service.client;

import com.order_service.dto.RestaurantSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "restaurant-catalog", url = "${restaurant-service.url}")
public interface RestaurantCatalogClient {

    @GetMapping("/api/restaurants/{id}")
    RestaurantSummaryDto getRestaurant(@PathVariable("id") Long id);
}
