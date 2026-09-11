package com.restaurent_service.restaurent_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RestaurantCreateRequest {

    @NotBlank
    private String name;

    private String description;
    private String addressLine1;
    private String city;
    private String cuisineType;
}
