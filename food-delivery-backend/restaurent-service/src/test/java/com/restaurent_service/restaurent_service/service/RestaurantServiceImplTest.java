package com.restaurent_service.restaurent_service.service;

import com.restaurent_service.restaurent_service.dto.MenuItemDto;
import com.restaurent_service.restaurent_service.dto.request.CategoryRequest;
import com.restaurent_service.restaurent_service.dto.request.MenuItemCreateRequest;
import com.restaurent_service.restaurent_service.entity.Category;
import com.restaurent_service.restaurent_service.entity.MenuItem;
import com.restaurent_service.restaurent_service.entity.Restaurant;
import com.restaurent_service.restaurent_service.entity.enums.FoodType;
import com.restaurent_service.restaurent_service.entity.enums.RestaurantStatus;
import com.restaurent_service.restaurent_service.exception.ResourceNotFoundException;
import com.restaurent_service.restaurent_service.repository.CategoryRepository;
import com.restaurent_service.restaurent_service.repository.MenuItemRepository;
import com.restaurent_service.restaurent_service.repository.RestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private LocalImageStorageService localImageStorageService;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private Restaurant restaurant;
    private Category mains;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin@test.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(1L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        restaurant = Restaurant.builder()
                .id(1L)
                .name("Demo Bistro")
                .status(RestaurantStatus.ACTIVE)
                .build();
        mains = Category.builder()
                .id(10L)
                .name("Mains")
                .restaurant(restaurant)
                .build();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPublicMenu_groupsItemsByCategoryName() {
        MenuItem pizza = MenuItem.builder()
                .id(100L)
                .restaurant(restaurant)
                .category(mains)
                .name("Pizza")
                .description("Cheese")
                .price(new BigDecimal("15.00"))
                .foodType(FoodType.VEG)
                .available(true)
                .build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurant_IdWithCategory(1L)).thenReturn(List.of(pizza));

        Map<String, List<MenuItemDto>> menu = restaurantService.getPublicMenu(1L);

        assertEquals(1, menu.size());
        assertTrue(menu.containsKey("Mains"));
        assertEquals(1, menu.get("Mains").size());
        assertEquals("Pizza", menu.get("Mains").get(0).getName());
        assertEquals(new BigDecimal("15.00"), menu.get("Mains").get(0).getPrice());
    }

    @Test
    void getPublicMenu_throwsWhenRestaurantMissing() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> restaurantService.getPublicMenu(99L));
        verify(menuItemRepository, never()).findByRestaurant_IdWithCategory(99L);
    }

    @Test
    void addCategory_throwsWhenDuplicateName() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Mains");
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.existsByRestaurant_IdAndNameIgnoreCase(1L, "Mains")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> restaurantService.addCategory(1L, request));
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void addMenuItem_persistsWithDefaultAvailabilityWhenNull() {
        MenuItemCreateRequest request = new MenuItemCreateRequest();
        request.setCategoryId(10L);
        request.setName("Burger");
        request.setPrice(new BigDecimal("12.00"));
        request.setFoodType("non_veg");
        request.setIsAvailable(null);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findByIdAndRestaurant_Id(10L, 1L)).thenReturn(Optional.of(mains));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        var dto = restaurantService.addMenuItem(1L, request);

        assertEquals(200L, dto.getId());
        assertEquals(Boolean.TRUE, dto.getIsAvailable());
        verify(menuItemRepository).save(any(MenuItem.class));
    }
}
