package com.auctionhouse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Web MVC configuration - serves uploaded files (profiles + auction images)
 * and configures aggressive browser caching for static assets.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ===== STATIC ASSETS (CSS, JS, images) — cache 1 year =====
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        // ===== UPLOADED FILES =====

        // Auction images — cache 30 days (they rarely change)
        String auctionPath = Paths.get(uploadDir)
                .getParent().resolve("auctions").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/auctions/**")
                .addResourceLocations(auctionPath)
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());

        // Payment proof files — cache 7 days
        String proofPath = Paths.get(uploadDir)
                .getParent().resolve("proof").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/proof/**")
                .addResourceLocations(proofPath)
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());

        // Profile pictures — cache 1 day (users change them occasionally)
        String profilePath = Paths.get(uploadDir)
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(profilePath)
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
    }
}
