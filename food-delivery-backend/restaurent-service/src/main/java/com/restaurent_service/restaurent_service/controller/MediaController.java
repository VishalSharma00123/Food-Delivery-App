package com.restaurent_service.restaurent_service.controller;

import com.restaurent_service.restaurent_service.service.LocalImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/public/media")
@RequiredArgsConstructor
public class MediaController {

    private final LocalImageStorageService imageStorageService;

    @GetMapping("/menu/{restaurantId}/{filename}")
    public ResponseEntity<Resource> getMenuImage(
            @PathVariable Long restaurantId,
            @PathVariable String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        String relative = "menu/" + restaurantId + "/" + filename;
        Resource resource = imageStorageService.loadAsResource(relative);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = imageStorageService.detectMediaType(filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
