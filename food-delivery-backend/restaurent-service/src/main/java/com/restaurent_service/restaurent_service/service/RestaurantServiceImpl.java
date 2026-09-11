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
import com.restaurent_service.restaurent_service.entity.Category;
import com.restaurent_service.restaurent_service.entity.MenuItem;
import com.restaurent_service.restaurent_service.entity.Restaurant;
import com.restaurent_service.restaurent_service.entity.enums.FoodType;
import com.restaurent_service.restaurent_service.entity.enums.RestaurantStatus;
import com.restaurent_service.restaurent_service.exception.ResourceNotFoundException;
import com.restaurent_service.restaurent_service.repository.CategoryRepository;
import com.fooddelivery.rbac.RbacSupport;
import com.fooddelivery.rbac.SecurityRoleUtils;
import com.restaurent_service.restaurent_service.repository.MenuItemRepository;
import com.restaurent_service.restaurent_service.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final LocalImageStorageService localImageStorageService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<MenuItemDto>> getPublicMenu(Long restaurantId) {
        loadRestaurant(restaurantId);
        List<MenuItem> items = menuItemRepository.findByRestaurant_IdWithCategory(restaurantId);
        Map<String, List<MenuItemDto>> grouped = new LinkedHashMap<>();
        for (MenuItem item : items) {
            String categoryName = item.getCategory().getName();
            grouped.computeIfAbsent(categoryName, key -> new ArrayList<>()).add(toMenuItemDto(item, restaurantId));
        }
        return grouped;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantDto> listRestaurants() {
        return restaurantRepository.findAll().stream().map(this::toRestaurantDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDto getRestaurant(Long id) {
        return toRestaurantDto(loadRestaurant(id));
    }

    @Override
    @Transactional
    public RestaurantDto createRestaurant(RestaurantCreateRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .status(RestaurantStatus.ACTIVE)
                .addressLine1(trimToNull(request.getAddressLine1()))
                .city(trimToNull(request.getCity()))
                .cuisineType(trimToNull(request.getCuisineType()))
                .ownerId(RbacSupport.requireUserId())
                .build();
        return toRestaurantDto(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public RestaurantDto updateRestaurant(Long id, RestaurantUpdateRequest request) {
        Restaurant restaurant = loadRestaurant(id);
        assertCanManageRestaurant(restaurant);
        if (request.getName() != null && !request.getName().isBlank()) {
            restaurant.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            restaurant.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getAddressLine1() != null) {
            restaurant.setAddressLine1(trimToNull(request.getAddressLine1()));
        }
        if (request.getCity() != null) {
            restaurant.setCity(trimToNull(request.getCity()));
        }
        if (request.getCuisineType() != null) {
            restaurant.setCuisineType(trimToNull(request.getCuisineType()));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            restaurant.setStatus(RestaurantStatus.valueOf(request.getStatus().trim().toUpperCase()));
        }
        return toRestaurantDto(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = loadRestaurant(id);
        assertCanManageRestaurant(restaurant);
        restaurantRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories(Long restaurantId) {
        loadRestaurant(restaurantId);
        return categoryRepository.findByRestaurant_IdOrderByNameAsc(restaurantId).stream()
                .map(this::toCategoryDto)
                .toList();
    }

    @Override
    @Transactional
    public CategoryDto addCategory(Long restaurantId, CategoryRequest request) {
        Restaurant restaurant = loadRestaurant(restaurantId);
        assertCanManageRestaurant(restaurant);
        String name = request.getName().trim();
        if (categoryRepository.existsByRestaurant_IdAndNameIgnoreCase(restaurantId, name)) {
            throw new IllegalArgumentException("Category name already exists for this restaurant");
        }
        Category category = Category.builder()
                .restaurant(restaurant)
                .name(name)
                .build();
        return toCategoryDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long restaurantId, Long categoryId, CategoryRequest request) {
        Category category = loadCategory(restaurantId, categoryId);
        assertCanManageRestaurant(category.getRestaurant());
        String name = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(name)
                && categoryRepository.existsByRestaurant_IdAndNameIgnoreCase(restaurantId, name)) {
            throw new IllegalArgumentException("Category name already exists for this restaurant");
        }
        category.setName(name);
        return toCategoryDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long restaurantId, Long categoryId) {
        Category category = loadCategory(restaurantId, categoryId);
        assertCanManageRestaurant(category.getRestaurant());
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemDto> listMenuItems(Long restaurantId) {
        loadRestaurant(restaurantId);
        return menuItemRepository.findByRestaurant_IdWithCategory(restaurantId).stream()
                .map(item -> toMenuItemDto(item, restaurantId))
                .toList();
    }

    @Override
    @Transactional
    public MenuItemDto addMenuItem(Long restaurantId, MenuItemCreateRequest request) {
        Restaurant restaurant = loadRestaurant(restaurantId);
        assertCanManageRestaurant(restaurant);
        Category category = loadCategory(restaurantId, request.getCategoryId());
        Boolean available = request.getIsAvailable() == null ? Boolean.TRUE : request.getIsAvailable();
        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .price(request.getPrice())
                .foodType(parseFoodType(request.getFoodType()))
                .available(Boolean.TRUE.equals(available))
                .imageUrl(trimToNull(request.getImageUrl()))
                .build();
        return toMenuItemDto(menuItemRepository.save(item), restaurantId);
    }

    @Override
    @Transactional
    public MenuItemDto updateMenuItem(Long restaurantId, Long menuItemId, MenuItemUpdateRequest request) {
        MenuItem item = loadMenuItem(restaurantId, menuItemId);
        assertCanManageRestaurant(item.getRestaurant());
        Category category = loadCategory(restaurantId, request.getCategoryId());
        item.setCategory(category);
        item.setName(request.getName().trim());
        item.setDescription(trimToNull(request.getDescription()));
        item.setPrice(request.getPrice());
        item.setFoodType(parseFoodType(request.getFoodType()));
        item.setAvailable(request.getIsAvailable());
        item.setImageUrl(trimToNull(request.getImageUrl()));
        return toMenuItemDto(menuItemRepository.save(item), restaurantId);
    }

    @Override
    @Transactional
    public void deleteMenuItem(Long restaurantId, Long menuItemId) {
        MenuItem item = loadMenuItem(restaurantId, menuItemId);
        assertCanManageRestaurant(item.getRestaurant());
        menuItemRepository.delete(item);
    }

    @Override
    @Transactional
    public MenuItemDto setAvailability(Long restaurantId, Long menuItemId, AvailabilityRequest request) {
        MenuItem item = loadMenuItem(restaurantId, menuItemId);
        assertCanManageRestaurant(item.getRestaurant());
        item.setAvailable(Boolean.TRUE.equals(request.getAvailable()));
        return toMenuItemDto(menuItemRepository.save(item), restaurantId);
    }

    @Override
    public String uploadMenuImage(Long restaurantId, MultipartFile file) {
        Restaurant restaurant = loadRestaurant(restaurantId);
        assertCanManageRestaurant(restaurant);
        return localImageStorageService.storeMenuImage(restaurantId, file);
    }

    private Restaurant loadRestaurant(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
    }

    private Category loadCategory(Long restaurantId, Long categoryId) {
        return categoryRepository.findByIdAndRestaurant_Id(categoryId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private MenuItem loadMenuItem(Long restaurantId, Long menuItemId) {
        return menuItemRepository.findByIdAndRestaurant_Id(menuItemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    private FoodType parseFoodType(String raw) {
        try {
            return FoodType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid foodType: " + raw);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * {@link SecurityRoleUtils#isAdmin()} may manage any restaurant.
     * {@link SecurityRoleUtils#isRestaurantOwner()} may only manage rows where {@code ownerId} matches JWT user id.
     */
    private void assertCanManageRestaurant(Restaurant restaurant) {
        if (SecurityRoleUtils.isAdmin()) {
            return;
        }
        if (SecurityRoleUtils.isRestaurantOwner()) {
            Long uid = RbacSupport.requireUserId();
            if (restaurant.getOwnerId() != null && restaurant.getOwnerId().equals(uid)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this restaurant");
    }

    private RestaurantDto toRestaurantDto(Restaurant restaurant) {
        return RestaurantDto.builder()
                .id(restaurant.getId())
                .ownerId(restaurant.getOwnerId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .status(restaurant.getStatus().name())
                .addressLine1(restaurant.getAddressLine1())
                .city(restaurant.getCity())
                .cuisineType(restaurant.getCuisineType())
                .createdAt(restaurant.getCreatedAt())
                .updatedAt(restaurant.getUpdatedAt())
                .build();
    }

    private CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    private MenuItemDto toMenuItemDto(MenuItem item, Long restaurantId) {
        return MenuItemDto.builder()
                .id(item.getId())
                .restaurantId(restaurantId)
                .categoryId(item.getCategory().getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .foodType(item.getFoodType().name())
                .isAvailable(Boolean.TRUE.equals(item.getAvailable()))
                .imageUrl(item.getImageUrl())
                .build();
    }
}
