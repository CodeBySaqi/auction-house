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
    private Path proofUploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        auctionUploadPath = Paths.get(uploadDir).getParent().resolve("auctions").toAbsolutePath().normalize();
        proofUploadPath = Paths.get(uploadDir).getParent().resolve("proof").toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            Files.createDirectories(auctionUploadPath);
            Files.createDirectories(proofUploadPath);
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
     * Store a payment proof file (delivery/receipt) and return the filename.
     * @param file the uploaded file
     * @param prefix a descriptive prefix (e.g. "seller_123" or "buyer_456")
     */
    public String storeProofFile(MultipartFile file, String prefix) throws IOException {
        validateImage(file);
        String extension = extensionForContentType(file.getContentType());
        String filename = prefix + "_" + UUID.randomUUID().toString() + extension;
        Path targetPath = proofUploadPath.resolve(filename);
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

        // SECURITY: verify actual file content (magic bytes), not just the
        // client-controlled Content-Type header. Blocks disguised uploads
        // (e.g. HTML/JS renamed to .png) from being served to other users.
        if (!hasValidImageMagicBytes(file)) {
            throw new IOException("File content is not a valid image");
        }
    }

    /**
     * Checks the file's leading bytes against known image signatures:
     * JPEG (FF D8 FF), PNG (89 50 4E 47 ...), GIF (GIF87a/GIF89a),
     * WebP (RIFF....WEBP).
     */
    private boolean hasValidImageMagicBytes(MultipartFile file) throws IOException {
        byte[] b = new byte[12];
        int n = 0;
        try (java.io.InputStream in = file.getInputStream()) {
            while (n < b.length) {
                int r = in.read(b, n, b.length - n);
                if (r == -1) break;
                n += r;
            }
        }
        if (n < 6) return false;

        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true;

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return true;

        // GIF: "GIF87a" or "GIF89a"
        String head6 = new String(b, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
        if (head6.equals("GIF87a") || head6.equals("GIF89a")) return true;

        // WebP: "RIFF" + 4 bytes + "WEBP"
        if (n >= 12 && new String(b, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(b, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) return true;

        return false;
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
