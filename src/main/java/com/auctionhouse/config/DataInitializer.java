package com.auctionhouse.config;

import com.auctionhouse.service.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default data on application startup.
 * Ensures a SUPER_ADMIN account exists.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final SuperAdminService superAdminService;

    @Autowired
    public DataInitializer(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @Override
    public void run(String... args) throws Exception {
        superAdminService.ensureSuperAdminExists();
    }
}
