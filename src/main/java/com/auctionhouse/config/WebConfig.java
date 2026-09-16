package com.auctionhouse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC configuration - serves uploaded files (profiles + auction images).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Auction images (more specific path first)
        String auctionPath = Paths.get(uploadDir)
                .getParent().resolve("auctions").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/auctions/**")
                .addResourceLocations(auctionPath);

        // Payment proof files (delivery/receipt proofs)
        String proofPath = Paths.get(uploadDir)
                .getParent().resolve("proof").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/proof/**")
                .addResourceLocations(proofPath);

        // Profile pictures (catch-all for /uploads/*)
        String profilePath = Paths.get(uploadDir)
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(profilePath);
    }
}
