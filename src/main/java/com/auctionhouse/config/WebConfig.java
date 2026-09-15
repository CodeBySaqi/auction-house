package com.auctionhouse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC configuration - serves uploaded files (profiles + auction images).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Auction images (more specific path first)
        String auctionPath = Paths.get(System.getProperty("app.upload.dir", "./uploads/profiles"))
                .getParent().resolve("auctions").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/auctions/**")
                .addResourceLocations(auctionPath);

        // Profile pictures (catch-all for /uploads/*)
        String profilePath = Paths.get(System.getProperty("app.upload.dir", "./uploads/profiles"))
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(profilePath);
    }
}
