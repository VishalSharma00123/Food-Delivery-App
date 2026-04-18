package com.restaurent_service.restaurent_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategoryDto {
    private Long id;
    private Long restaurantId;
    private String name;
    private String description;
}
