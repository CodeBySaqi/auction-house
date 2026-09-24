package com.auctionhouse.config;

import com.auctionhouse.model.User;
import com.auctionhouse.repository.UserRepository;
import com.auctionhouse.service.SuperAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default data on application startup.
 * - Ensures a SUPER_ADMIN account exists.
 * - Ensures the demo account advertised on the login page
 *   (johndoe / password123) actually exists.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SuperAdminService superAdminService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(SuperAdminService superAdminService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.superAdminService = superAdminService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        superAdminService.ensureSuperAdminExists();
        ensureDemoUserExists();
    }

    /**
     * The login page advertises "johndoe / password123" as demo credentials.
     * Seeded users get random names, so those credentials never worked and
     * anyone trying them got "Invalid username or password". Create the
     * account on startup if it is missing.
     */
    private void ensureDemoUserExists() {
        if (userRepository.findByUsername("johndoe").isPresent()) {
            return;
        }
        User demo = new User();
        demo.setUsername("johndoe");
        demo.setEmail("johndoe@auctionhouse.com");
        demo.setPassword(passwordEncoder.encode("password123"));
        demo.setWalletBalance(25000.0);
        demo.setActive(true);
        userRepository.save(demo);
        log.info("Demo account 'johndoe' created (password123)");
    }
}
