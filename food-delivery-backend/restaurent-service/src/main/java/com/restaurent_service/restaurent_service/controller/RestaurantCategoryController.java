package com.restaurent_service.restaurent_service.controller;

import com.restaurent_service.restaurent_service.dto.CategoryDto;
import com.restaurent_service.restaurent_service.dto.request.CategoryRequest;
import com.restaurent_service.restaurent_service.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/categories")
@RequiredArgsConstructor
public class RestaurantCategoryController {

    private final RestaurantService restaurantService;

    @GetMapping
    public List<CategoryDto> listCategories(@PathVariable Long restaurantId) {
        return restaurantService.listCategories(restaurantId);
    }

    @PostMapping
    public ResponseEntity<CategoryDto> addCategory(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CategoryRequest request) {
        CategoryDto created = restaurantService.addCategory(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{categoryId}")
    public CategoryDto updateCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        return restaurantService.updateCategory(restaurantId, categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId) {
        restaurantService.deleteCategory(restaurantId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
