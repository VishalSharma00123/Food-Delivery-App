package com.restaurent_service.restaurent_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityRequest {

    @NotNull
    private Boolean available;
}
