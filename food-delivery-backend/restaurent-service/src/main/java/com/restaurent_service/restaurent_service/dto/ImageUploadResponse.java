package com.restaurent_service.restaurent_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {
    /** Public path served via gateway, e.g. /api/public/media/menu/1/uuid.jpg */
    private String imageUrl;
}
