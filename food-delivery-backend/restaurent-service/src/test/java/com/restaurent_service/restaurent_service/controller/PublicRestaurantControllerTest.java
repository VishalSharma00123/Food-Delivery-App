package com.restaurent_service.restaurent_service.controller;

import com.fooddelivery.rbac.JwtService;
import com.restaurent_service.restaurent_service.dto.MenuItemDto;
import com.restaurent_service.restaurent_service.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicRestaurantController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class PublicRestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private JwtService jwtService;

    @Test
    void getMenu_returnsGroupedJson() throws Exception {
        MenuItemDto dto = MenuItemDto.builder()
                .id(1L)
                .restaurantId(5L)
                .categoryId(10L)
                .name("Cola")
                .price(new BigDecimal("3.00"))
                .foodType("VEG")
                .isAvailable(true)
                .build();
        when(restaurantService.getPublicMenu(5L)).thenReturn(Map.of("Drinks", List.of(dto)));

        mockMvc.perform(get("/api/public/restaurants/5/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Drinks[0].name").value("Cola"))
                .andExpect(jsonPath("$.Drinks[0].isAvailable").value(true));
    }
}
