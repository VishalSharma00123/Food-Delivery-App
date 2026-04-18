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
    private String address;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
}
