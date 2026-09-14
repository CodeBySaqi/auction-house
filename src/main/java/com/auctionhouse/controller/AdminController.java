package com.auctionhouse.controller;

import com.auctionhouse.model.User;
import com.auctionhouse.service.AuctionService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin controller for managing the auction platform.
 * Only accessible to users with ROLE_ADMIN.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AuctionService auctionService;

    @Autowired
    public AdminController(UserService userService, AuctionService auctionService) {
        this.userService = userService;
        this.auctionService = auctionService;
    }

    /**
     * Admin dashboard - main overview page
     */
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Principal principal) {
        // Get current admin user
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        model.addAttribute("admin", admin);

        // Get statistics
        List<User> allUsers = userService.findAllUsers();
        model.addAttribute("totalUsers", allUsers.size());

        // Get auction statistics
        model.addAttribute("totalAuctions", auctionService.getAllAuctions().size());
        model.addAttribute("activeAuctions", auctionService.getActiveAuctions().size());

        // Get recent activity (last 5 users) with formatted dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        List<User> recentUsers = allUsers.stream()
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("recentUsers", recentUsers);
        model.addAttribute("dateFormatter", formatter);

        return "admin-dashboard";
    }
}
