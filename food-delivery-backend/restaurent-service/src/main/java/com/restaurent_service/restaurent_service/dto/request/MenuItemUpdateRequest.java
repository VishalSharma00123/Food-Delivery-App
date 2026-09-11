package com.restaurent_service.restaurent_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MenuItemUpdateRequest {

    @NotNull
    private Long categoryId;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotBlank
    private String foodType;

    @NotNull
    private Boolean isAvailable;

    private String imageUrl;
}
