package com.restaurent_service.restaurent_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDto {
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private String status;
    private String addressLine1;
    private String city;
    private String cuisineType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
