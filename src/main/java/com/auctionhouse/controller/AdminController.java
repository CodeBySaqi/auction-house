package com.auctionhouse.controller;

import com.auctionhouse.model.*;
import com.auctionhouse.service.AuctionService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private final PasswordEncoder passwordEncoder;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Autowired
    public AdminController(UserService userService, AuctionService auctionService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Admin dashboard - main overview page
     */
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Principal principal) {
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        model.addAttribute("admin", admin);

        List<User> allUsers = userService.findAllUsers();
        List<Auction> allAuctions = auctionService.getAllAuctions();
        List<Auction> activeAuctions = auctionService.getActiveAuctions();

        // Stats
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalAuctions", allAuctions.size());
        model.addAttribute("activeAuctions", activeAuctions.size());

        // Total revenue
        double totalRevenue = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED)
                .mapToDouble(Auction::getCurrentHighestBid)
                .sum();
        model.addAttribute("totalRevenue", totalRevenue);

        // Recent users (last 5)
        List<User> recentUsers = allUsers.stream()
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentUsers", recentUsers);

        // Recent auctions (last 5)
        List<Auction> recentAuctions = allAuctions.stream()
                .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentAuctions", recentAuctions);

        // Category stats for chart
        Map<String, Long> categoryStats = allAuctions.stream()
                .collect(Collectors.groupingBy(a -> a.getCategory().name(), Collectors.counting()));
        model.addAttribute("categoryStats", categoryStats);

        // Monthly revenue data (last 6 months)
        Map<String, Double> monthlyRevenue = getMonthlyRevenue(allAuctions);
        model.addAttribute("monthlyRevenue", monthlyRevenue);

        model.addAttribute("dateFormatter", dateFormatter);
        model.addAttribute("dateTimeFormatter", dateTimeFormatter);
        model.addAttribute("activeTab", "dashboard");

        return "admin-dashboard";
    }

    /**
     * Manage auctions page
     */
    @GetMapping("/auctions")
    public String manageAuctions(Model model, Principal principal,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String search) {
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        model.addAttribute("admin", admin);

        List<Auction> auctions = auctionService.getAllAuctions();

        // Filter by status
        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            try {
                AuctionStatus auctionStatus = AuctionStatus.valueOf(status);
                auctions = auctions.stream()
                        .filter(a -> a.getStatus() == auctionStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid status, show all
            }
        }

        // Filter by search
        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            auctions = auctions.stream()
                    .filter(a -> a.getTitle().toLowerCase().contains(lowerSearch)
                            || a.getDescription().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }

        auctions.sort((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()));

        model.addAttribute("auctions", auctions);
        model.addAttribute("currentStatus", status != null ? status : "ALL");
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("dateTimeFormatter", dateTimeFormatter);
        model.addAttribute("activeTab", "auctions");

        return "admin-dashboard";
    }

    /**
     * Delete auction
     */
    @PostMapping("/auctions/delete/{id}")
    public String deleteAuction(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            auctionService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Auction deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete auction: " + e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    /**
     * Toggle auction status (active/closed)
     */
    @PostMapping("/auctions/toggle/{id}")
    public String toggleAuctionStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Auction> auctionOpt = auctionService.findById(id);
            if (auctionOpt.isPresent()) {
                Auction auction = auctionOpt.get();
                if (auction.getStatus() == AuctionStatus.ACTIVE) {
                    auction.setStatus(AuctionStatus.CLOSED);
                    redirectAttributes.addFlashAttribute("successMessage", "Auction closed!");
                } else {
                    auction.setStatus(AuctionStatus.ACTIVE);
                    auction.setEndTime(LocalDateTime.now().plusDays(7));
                    redirectAttributes.addFlashAttribute("successMessage", "Auction reactivated!");
                }
                auctionService.save(auction);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update auction: " + e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    /**
     * Manage users page
     */
    @GetMapping("/users")
    public String manageUsers(Model model, Principal principal,
                               @RequestParam(required = false) String search) {
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        model.addAttribute("admin", admin);

        List<User> users = userService.findAllUsers();

        // Filter by search
        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            users = users.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(lowerSearch)
                            || u.getEmail().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }

        users.sort((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()));

        model.addAttribute("users", users);
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("dateFormatter", dateFormatter);
        model.addAttribute("activeTab", "users");

        return "admin-dashboard";
    }

    /**
     * Create new user with role selection
     */
    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                              @RequestParam String email,
                              @RequestParam String password,
                              @RequestParam String role,
                              RedirectAttributes redirectAttributes) {
        try {
            // Check if username or email already exists
            if (userService.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Username '" + username + "' already exists!");
                return "redirect:/admin/users";
            }
            if (userService.findByEmail(email).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email '" + email + "' already registered!");
                return "redirect:/admin/users";
            }

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setRole(role);
            newUser.setWalletBalance(100000.0);
            userService.updateProfile(newUser);

            String roleName = role.equals("ROLE_ADMIN") ? "Admin" : "User";
            redirectAttributes.addFlashAttribute("successMessage",
                    "New " + roleName + " '" + username + "' created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Change user role
     */
    @PostMapping("/users/role/{id}")
    public String changeUserRole(@PathVariable Long id,
                                  @RequestParam String role,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getUsername().equals(principal.getName())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Cannot change your own role!");
                    return "redirect:/admin/users";
                }
                String oldRole = user.getRole().replace("ROLE_", "");
                String newRole = role.replace("ROLE_", "");
                user.setRole(role);
                userService.updateProfile(user);
                redirectAttributes.addFlashAttribute("successMessage",
                        user.getUsername() + "'s role changed from " + oldRole + " to " + newRole + "!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to change role: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Toggle user status (ban/unban)
     */
    @PostMapping("/users/toggle/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if ("ROLE_ADMIN".equals(user.getRole())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Cannot ban an admin user!");
                    return "redirect:/admin/users";
                }
                if ("ROLE_BANNED".equals(user.getRole())) {
                    user.setRole("ROLE_USER");
                    redirectAttributes.addFlashAttribute("successMessage", "User '" + user.getUsername() + "' unbanned!");
                } else {
                    user.setRole("ROLE_BANNED");
                    redirectAttributes.addFlashAttribute("successMessage", "User '" + user.getUsername() + "' banned!");
                }
                userService.updateProfile(user);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Delete user
     */
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getUsername().equals(principal.getName())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete your own account!");
                    return "redirect:/admin/users";
                }
                userService.deleteById(id);
                redirectAttributes.addFlashAttribute("successMessage", "User '" + user.getUsername() + "' deleted!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Adjust user balance
     */
    @PostMapping("/users/balance/{id}")
    public String adjustBalance(@PathVariable Long id,
                                 @RequestParam double amount,
                                 RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setWalletBalance(user.getWalletBalance() + amount);
                userService.updateProfile(user);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Added $" + String.format("%,.0f", amount) + " to " + user.getUsername() +
                        ". New balance: $" + String.format("%,.2f", user.getWalletBalance()));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update balance: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Analytics page
     */
    @GetMapping("/analytics")
    public String analytics(Model model, Principal principal) {
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        model.addAttribute("admin", admin);

        List<Auction> allAuctions = auctionService.getAllAuctions();
        List<User> allUsers = userService.findAllUsers();

        // Total users
        model.addAttribute("totalUsers", allUsers.size());

        // Category distribution
        Map<String, Long> categoryStats = allAuctions.stream()
                .collect(Collectors.groupingBy(a -> a.getCategory().name(), Collectors.counting()));
        model.addAttribute("categoryStats", categoryStats);

        // Monthly revenue
        Map<String, Double> monthlyRevenue = getMonthlyRevenue(allAuctions);
        model.addAttribute("monthlyRevenue", monthlyRevenue);

        // Status distribution
        Map<String, Long> statusStats = allAuctions.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));
        model.addAttribute("statusStats", statusStats);

        // Top bidders
        Map<String, Integer> topBidders = allUsers.stream()
                .filter(u -> u.getBids() != null && !u.getBids().isEmpty())
                .sorted((u1, u2) -> Integer.compare(u2.getBids().size(), u1.getBids().size()))
                .limit(10)
                .collect(Collectors.toMap(User::getUsername, u -> u.getBids().size(),
                        (e1, e2) -> e1, LinkedHashMap::new));
        model.addAttribute("topBidders", topBidders);

        // Total stats
        double totalRevenue = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED)
                .mapToDouble(Auction::getCurrentHighestBid)
                .sum();
        int totalBids = allAuctions.stream().mapToInt(Auction::getBidCount).sum();
        double avgBidPerAuction = allAuctions.isEmpty() ? 0 : (double) totalBids / allAuctions.size();

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalBids", totalBids);
        model.addAttribute("avgBidPerAuction", avgBidPerAuction);
        model.addAttribute("activeTab", "analytics");

        return "admin-dashboard";
    }

    /**
     * Settings page
     */
    @GetMapping("/settings")
    public String settings(Model model, Principal principal) {
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        model.addAttribute("admin", admin);
        model.addAttribute("activeTab", "settings");
        return "admin-dashboard";
    }

    /**
     * Save settings
     */
    @PostMapping("/settings")
    public String saveSettings(@RequestParam String siteName,
                                @RequestParam String adminEmail,
                                @RequestParam double defaultBalance,
                                @RequestParam int defaultDuration,
                                RedirectAttributes redirectAttributes) {
        try {
            // In a real app, these would be saved to a settings table or config
            // For now, we just show a success message
            redirectAttributes.addFlashAttribute("successMessage", "Settings saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save settings: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    /**
     * Helper: Get monthly revenue for last 6 months
     */
    private Map<String, Double> getMonthlyRevenue(List<Auction> auctions) {
        Map<String, Double> monthlyRevenue = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            String monthLabel = monthStart.format(DateTimeFormatter.ofPattern("MMM"));

            double revenue = auctions.stream()
                    .filter(a -> a.getStatus() == AuctionStatus.CLOSED
                            && a.getEndTime() != null
                            && !a.getEndTime().isBefore(monthStart)
                            && !a.getEndTime().isAfter(monthEnd))
                    .mapToDouble(Auction::getCurrentHighestBid)
                    .sum();

            monthlyRevenue.put(monthLabel, revenue);
        }
        return monthlyRevenue;
    }
}
