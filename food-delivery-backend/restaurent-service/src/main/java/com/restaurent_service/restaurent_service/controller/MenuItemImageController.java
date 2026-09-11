package com.restaurent_service.restaurent_service.controller;

import com.restaurent_service.restaurent_service.dto.ImageUploadResponse;
import com.restaurent_service.restaurent_service.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menu-items")
@RequiredArgsConstructor
public class MenuItemImageController {

    private final RestaurantService restaurantService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadImage(
            @PathVariable Long restaurantId,
            @RequestPart("file") MultipartFile file) {
        String imageUrl = restaurantService.uploadMenuImage(restaurantId, file);
        return ImageUploadResponse.builder().imageUrl(imageUrl).build();
    }
}
