package com.restaurent_service.restaurent_service.controller;

import com.restaurent_service.restaurent_service.dto.MenuItemDto;
import com.restaurent_service.restaurent_service.dto.request.AvailabilityRequest;
import com.restaurent_service.restaurent_service.dto.request.MenuItemCreateRequest;
import com.restaurent_service.restaurent_service.dto.request.MenuItemUpdateRequest;
import com.restaurent_service.restaurent_service.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menu-items")
@RequiredArgsConstructor
public class RestaurantMenuItemController {

    private final RestaurantService restaurantService;

    @GetMapping
    public List<MenuItemDto> listMenuItems(@PathVariable Long restaurantId) {
        return restaurantService.listMenuItems(restaurantId);
    }

    @PostMapping
    public ResponseEntity<MenuItemDto> addMenuItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuItemCreateRequest request) {
        MenuItemDto created = restaurantService.addMenuItem(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{menuItemId}")
    public MenuItemDto updateMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @Valid @RequestBody MenuItemUpdateRequest request) {
        return restaurantService.updateMenuItem(restaurantId, menuItemId, request);
    }

    @PatchMapping("/{menuItemId}/availability")
    public MenuItemDto updateAvailability(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @Valid @RequestBody AvailabilityRequest request) {
        return restaurantService.setAvailability(restaurantId, menuItemId, request);
    }

    @DeleteMapping("/{menuItemId}")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId) {
        restaurantService.deleteMenuItem(restaurantId, menuItemId);
        return ResponseEntity.noContent().build();
    }
}
