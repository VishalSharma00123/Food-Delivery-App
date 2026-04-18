package com.restaurent_service.restaurent_service.controller;

import com.restaurent_service.restaurent_service.dto.MenuCategoryDto;
import com.restaurent_service.restaurent_service.dto.MenuItemDto;
import com.restaurent_service.restaurent_service.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MenuController {

    @Autowired
    private MenuService menuService;

    // Public/Customer Facing APIs
    @GetMapping("/public/restaurants/{id}/menu")
    public ResponseEntity<Map<String, List<MenuItemDto>>> getRestaurantMenu(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getGroupedMenuForRestaurant(id));
    }

    // Admin/Restaurant Owner APIs
    @PostMapping("/restaurants/{id}/categories")
    public ResponseEntity<MenuCategoryDto> addCategory(@PathVariable Long id, @RequestBody MenuCategoryDto dto) {
        return ResponseEntity.ok(menuService.createCategory(id, dto));
    }

    @PostMapping("/restaurants/{id}/items")
    public ResponseEntity<MenuItemDto> addItem(@PathVariable Long id, @RequestBody MenuItemDto dto) {
        return ResponseEntity.ok(menuService.createMenuItem(id, dto));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<MenuItemDto> updateItem(@PathVariable Long itemId, @RequestBody MenuItemDto dto) {
        return ResponseEntity.ok(menuService.updateMenuItem(itemId, dto));
    }

    @PatchMapping("/items/{itemId}/availability")
    public ResponseEntity<MenuItemDto> updateAvailability(@PathVariable Long itemId, @RequestBody Map<String, Boolean> body) {
        Boolean isAvailable = body.get("isAvailable");
        return ResponseEntity.ok(menuService.updateItemAvailability(itemId, isAvailable));
    }
}
