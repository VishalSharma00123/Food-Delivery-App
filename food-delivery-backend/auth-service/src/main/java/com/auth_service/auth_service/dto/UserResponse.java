package com.auth_service.auth_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private boolean enabled;
    private LocalDateTime createdAt;
    private List<String> roles;
}