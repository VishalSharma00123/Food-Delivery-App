package com.restaurent_service.restaurent_service.controller;

import com.restaurent_service.restaurent_service.dto.MenuItemDto;
import com.restaurent_service.restaurent_service.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/restaurants")
@RequiredArgsConstructor
public class PublicRestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/{restaurantId}/menu")
    public Map<String, List<MenuItemDto>> getMenu(@PathVariable Long restaurantId) {
        return restaurantService.getPublicMenu(restaurantId);
    }
}
