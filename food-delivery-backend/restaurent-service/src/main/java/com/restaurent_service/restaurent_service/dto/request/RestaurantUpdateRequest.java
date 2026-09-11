package com.restaurent_service.restaurent_service.dto.request;

import lombok.Data;

@Data
public class RestaurantUpdateRequest {
    private String name;
    private String description;
    private String status;
    private String addressLine1;
    private String city;
    private String cuisineType;
}
