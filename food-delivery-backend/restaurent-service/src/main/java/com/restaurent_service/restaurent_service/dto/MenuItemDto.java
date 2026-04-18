package com.restaurent_service.restaurent_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDto {
    private Long id;
    private Long restaurantId;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String foodType;
    private Boolean isAvailable;
    private String imageUrl;
}
