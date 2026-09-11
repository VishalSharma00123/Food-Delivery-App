package com.restaurent_service.restaurent_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class  CategoryRequest {

    @NotBlank
    private String name;
}
