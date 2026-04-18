package com.restaurent_service.restaurent_service.service;

import com.restaurent_service.restaurent_service.dto.MenuCategoryDto;
import com.restaurent_service.restaurent_service.dto.MenuItemDto;
import java.util.List;
import java.util.Map;

public interface MenuService {
    MenuCategoryDto createCategory(Long restaurantId, MenuCategoryDto categoryDto);
    List<MenuCategoryDto> getCategoriesForRestaurant(Long restaurantId);
    
    MenuItemDto createMenuItem(Long restaurantId, MenuItemDto menuItemDto);
    MenuItemDto updateMenuItem(Long itemId, MenuItemDto menuItemDto);
    MenuItemDto updateItemAvailability(Long itemId, Boolean isAvailable);
    
    List<MenuItemDto> getMenuItemsForRestaurant(Long restaurantId);
    Map<String, List<MenuItemDto>> getGroupedMenuForRestaurant(Long restaurantId);
}
