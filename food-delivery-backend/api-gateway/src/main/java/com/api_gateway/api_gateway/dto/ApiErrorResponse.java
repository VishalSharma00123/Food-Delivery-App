package com.api_gateway.api_gateway.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private boolean success;
    private String message;
    private String path;
    private int status;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ApiErrorResponse of(String message, String path, int status) {
        return ApiErrorResponse.builder()
                .success(false)
                .message(message)
                .path(path)
                .status(status)
                .build();
    }
}
