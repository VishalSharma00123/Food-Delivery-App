package com.restaurent_service.restaurent_service.service;

import com.restaurent_service.restaurent_service.dto.MenuCategoryDto;
import com.restaurent_service.restaurent_service.dto.MenuItemDto;
import com.restaurent_service.restaurent_service.entity.MenuCategory;
import com.restaurent_service.restaurent_service.entity.MenuItem;
import com.restaurent_service.restaurent_service.repository.MenuCategoryRepository;
import com.restaurent_service.restaurent_service.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Override
    public MenuCategoryDto createCategory(Long restaurantId, MenuCategoryDto dto) {
        MenuCategory category = MenuCategory.builder()
                .restaurantId(restaurantId)
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return mapToDto(categoryRepository.save(category));
    }

    @Override
    public List<MenuCategoryDto> getCategoriesForRestaurant(Long restaurantId) {
        return categoryRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public MenuItemDto createMenuItem(Long restaurantId, MenuItemDto dto) {
        MenuItem item = MenuItem.builder()
                .restaurantId(restaurantId)
                .categoryId(dto.getCategoryId())
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .foodType(dto.getFoodType())
                .isAvailable(true)
                .imageUrl(dto.getImageUrl())
                .build();
        return mapToDto(itemRepository.save(item));
    }

    @Override
    public MenuItemDto updateMenuItem(Long itemId, MenuItemDto dto) {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setFoodType(dto.getFoodType());
        item.setImageUrl(dto.getImageUrl());
        
        return mapToDto(itemRepository.save(item));
    }

    @Override
    public MenuItemDto updateItemAvailability(Long itemId, Boolean isAvailable) {
        MenuItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        item.setIsAvailable(isAvailable);
        return mapToDto(itemRepository.save(item));
    }

    @Override
    public List<MenuItemDto> getMenuItemsForRestaurant(Long restaurantId) {
        return itemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<MenuItemDto>> getGroupedMenuForRestaurant(Long restaurantId) {
        List<MenuItemDto> allItems = getMenuItemsForRestaurant(restaurantId);
        List<MenuCategory> categories = categoryRepository.findByRestaurantId(restaurantId);
        
        Map<Long, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(MenuCategory::getId, MenuCategory::getName));
        
        return allItems.stream()
                .collect(Collectors.groupingBy(item -> categoryNames.getOrDefault(item.getCategoryId(), "Uncategorized")));
    }

    private MenuCategoryDto mapToDto(MenuCategory category) {
        return MenuCategoryDto.builder()
                .id(category.getId())
                .restaurantId(category.getRestaurantId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    private MenuItemDto mapToDto(MenuItem item) {
        return MenuItemDto.builder()
                .id(item.getId())
                .restaurantId(item.getRestaurantId())
                .categoryId(item.getCategoryId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .foodType(item.getFoodType())
                .isAvailable(item.getIsAvailable())
                .imageUrl(item.getImageUrl())
                .build();
    }
}
