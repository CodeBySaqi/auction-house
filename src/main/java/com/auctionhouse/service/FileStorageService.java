package com.auctionhouse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * FileStorageService - handles profile picture and auction image uploads.
 */
@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;
    private Path auctionUploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        auctionUploadPath = Paths.get(uploadDir).getParent().resolve("auctions").toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            Files.createDirectories(auctionUploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * Store a profile picture and return the filename.
     */
    public String storeProfilePic(MultipartFile file) throws IOException {
        validateImage(file);
        String filename = generateFilename(file);
        Path targetPath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath);
        return filename;
    }

    /**
     * Store an auction image and return the filename.
     */
    public String storeAuctionImage(MultipartFile file) throws IOException {
        validateImage(file);
        String filename = generateFilename(file);
        Path targetPath = auctionUploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath);
        return filename;
    }

    /**
     * Validate uploaded image file.
     */
    private void validateImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") &&
                !contentType.equals("image/png") && !contentType.equals("image/gif") &&
                !contentType.equals("image/webp"))) {
            throw new IOException("Only JPEG, PNG, GIF, and WebP images are allowed");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IOException("File size must be less than 5MB");
        }
    }

    /**
     * Generate a unique filename using the content type to determine extension.
     * This avoids path traversal attacks from attacker-controlled original filenames.
     */
    private String generateFilename(MultipartFile file) {
        String extension = extensionForContentType(file.getContentType());
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Map a validated content type to a safe file extension.
     * validateImage() already restricts contentType to these four values.
     */
    private String extensionForContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type is required");
        }
        switch (contentType) {
            case "image/jpeg": return ".jpg";
            case "image/png":  return ".png";
            case "image/gif":  return ".gif";
            case "image/webp": return ".webp";
            default: throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
    }

    /**
     * Delete a profile picture.
     */
    public void deleteProfilePic(String filename) {
        try {
            Path filePath = uploadPath.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't throw
        }
    }
}
