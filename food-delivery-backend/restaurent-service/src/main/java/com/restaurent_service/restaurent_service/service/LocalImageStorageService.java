package com.restaurent_service.restaurent_service.service;

import com.restaurent_service.restaurent_service.config.UploadProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalImageStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );
    private static final Map<String, String> EXTENSION_BY_TYPE = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp"
    );

    private final Path rootDir;

    public LocalImageStorageService(UploadProperties uploadProperties) throws IOException {
        this.rootDir = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
        Files.createDirectories(this.rootDir);
    }

    public String storeMenuImage(Long restaurantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image must be 5MB or smaller");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP images are allowed");
        }

        String extension = EXTENSION_BY_TYPE.get(contentType);
        String filename = UUID.randomUUID() + extension;
        Path restaurantDir = rootDir.resolve("menu").resolve(String.valueOf(restaurantId)).normalize();
        if (!restaurantDir.startsWith(rootDir)) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        try {
            Files.createDirectories(restaurantDir);
            Path target = restaurantDir.resolve(filename).normalize();
            if (!target.startsWith(restaurantDir)) {
                throw new IllegalArgumentException("Invalid file name");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store image on disk", ex);
        }

        return "/api/public/media/menu/" + restaurantId + "/" + filename;
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path file = resolveSafe(relativePath);
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return null;
            }
            Resource resource = new UrlResource(file.toUri());
            return resource.exists() && resource.isReadable() ? resource : null;
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    public MediaType detectMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    private Path resolveSafe(String relativePath) {
        Path resolved = rootDir.resolve(relativePath).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new IllegalArgumentException("Invalid media path");
        }
        return resolved;
    }
}
