package com.restaurent_service.restaurent_service.service;

import com.restaurent_service.restaurent_service.dto.CategoryDto;
import com.restaurent_service.restaurent_service.dto.MenuItemDto;
import com.restaurent_service.restaurent_service.dto.RestaurantDto;
import com.restaurent_service.restaurent_service.dto.request.AvailabilityRequest;
import com.restaurent_service.restaurent_service.dto.request.CategoryRequest;
import com.restaurent_service.restaurent_service.dto.request.MenuItemCreateRequest;
import com.restaurent_service.restaurent_service.dto.request.MenuItemUpdateRequest;
import com.restaurent_service.restaurent_service.dto.request.RestaurantCreateRequest;
import com.restaurent_service.restaurent_service.dto.request.RestaurantUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RestaurantService {

    Map<String, List<MenuItemDto>> getPublicMenu(Long restaurantId);

    List<RestaurantDto> listRestaurants();

    RestaurantDto getRestaurant(Long id);

    RestaurantDto createRestaurant(RestaurantCreateRequest request);

    RestaurantDto updateRestaurant(Long id, RestaurantUpdateRequest request);

    void deleteRestaurant(Long id);

    List<CategoryDto> listCategories(Long restaurantId);

    CategoryDto addCategory(Long restaurantId, CategoryRequest request);

    CategoryDto updateCategory(Long restaurantId, Long categoryId, CategoryRequest request);

    void deleteCategory(Long restaurantId, Long categoryId);

    List<MenuItemDto> listMenuItems(Long restaurantId);

    MenuItemDto addMenuItem(Long restaurantId, MenuItemCreateRequest request);

    MenuItemDto updateMenuItem(Long restaurantId, Long menuItemId, MenuItemUpdateRequest request);

    void deleteMenuItem(Long restaurantId, Long menuItemId);

    MenuItemDto setAvailability(Long restaurantId, Long menuItemId, AvailabilityRequest request);

    /** Stores image on local disk and returns a public URL path (no S3). */
    String uploadMenuImage(Long restaurantId, MultipartFile file);
}
