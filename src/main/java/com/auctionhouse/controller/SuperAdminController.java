package com.auctionhouse.controller;

import com.auctionhouse.model.AdminAuditLog;
import com.auctionhouse.model.User;
import com.auctionhouse.service.AuditService;
import com.auctionhouse.service.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * SuperAdminController - Super Admin management dashboard.
 * Only accessible by ROLE_SUPER_ADMIN.
 */
@Controller
@RequestMapping("/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final AuditService auditService;

    @Autowired
    public SuperAdminController(SuperAdminService superAdminService, AuditService auditService) {
        this.superAdminService = superAdminService;
        this.auditService = auditService;
    }

    /**
     * Add shared model attributes needed by all super-admin pages (sidebar badges).
     */
    private void addSidebarCounts(Model model) {
        model.addAttribute("totalUsers", superAdminService.countTotalUsers());
        model.addAttribute("totalAdmins", superAdminService.countAdmins());
    }

    // ==================== DASHBOARD ====================

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User superAdmin = superAdminService.findUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Super Admin not found"));

        model.addAttribute("superAdmin", superAdmin);
        model.addAttribute("totalUsers", superAdminService.countTotalUsers());
        model.addAttribute("activeUsers", superAdminService.countActiveUsers());
        model.addAttribute("deactivatedUsers", superAdminService.countDeactivatedUsers());
        model.addAttribute("totalAdmins", superAdminService.countAdmins());
        model.addAttribute("totalSuperAdmins", superAdminService.countSuperAdmins());
        model.addAttribute("recentAuditLogs", superAdminService.getAuditLogs().stream().limit(10).toList());

        return "super-admin/dashboard";
    }

    // ==================== PROFILE ====================

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User superAdmin = superAdminService.findUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Super Admin not found"));
        model.addAttribute("superAdmin", superAdmin);
        addSidebarCounts(model);
        return "super-admin/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String username,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String currentPassword,
                                 @RequestParam(required = false) String newPassword,
                                 @RequestParam(required = false) String confirmNewPassword,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            User superAdmin = superAdminService.findUserByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Super Admin not found"));
            superAdminService.updateSuperAdminProfile(superAdmin.getId(), username, email,
                    currentPassword, newPassword, confirmNewPassword);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
        }
        return "redirect:/super-admin/profile";
    }

    // ==================== USER MANAGEMENT ====================

    @GetMapping("/users")
    public String manageUsers(@RequestParam(required = false) String search,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        List<User> users;
        if (search != null && !search.isEmpty()) {
            users = superAdminService.searchUsers(search);
        } else {
            users = superAdminService.getAllUsersOrderedByCreated();
        }

        model.addAttribute("users", users);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("currentSuperAdmin", superAdminService.findUserByUsername(userDetails.getUsername()).orElse(null));
        addSidebarCounts(model);

        return "super-admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User target = superAdminService.findUserById(id).orElse(null);
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/super-admin/users";
        }

        model.addAttribute("targetUser", target);
        model.addAttribute("auditLogs", superAdminService.getAuditLogsForUser(id));
        model.addAttribute("currentSuperAdmin", superAdminService.findUserByUsername(userDetails.getUsername()).orElse(null));
        addSidebarCounts(model);

        return "super-admin/user-detail";
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            User performedBy = superAdminService.findUserByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Super Admin not found"));
            superAdminService.activateUser(id, performedBy);
            redirectAttributes.addFlashAttribute("success", "User activated successfully.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "Not authorized: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/users/" + id;
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            User performedBy = superAdminService.findUserByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Super Admin not found"));
            superAdminService.deactivateUser(id, performedBy);
            redirectAttributes.addFlashAttribute("success", "User deactivated successfully. All data remains intact.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "Not authorized: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/users/" + id;
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable Long id,
                                  @RequestParam String role,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            User performedBy = superAdminService.findUserByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Super Admin not found"));
            superAdminService.changeUserRole(id, role, performedBy);
            redirectAttributes.addFlashAttribute("success", "Role updated successfully.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "Not authorized: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/users/" + id;
    }

    // ==================== ADMIN MANAGEMENT ====================

    @GetMapping("/admins")
    public String manageAdmins(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("admins", superAdminService.getAdmins());
        model.addAttribute("superAdmins", superAdminService.getSuperAdmins());
        model.addAttribute("currentSuperAdmin", superAdminService.findUserByUsername(userDetails.getUsername()).orElse(null));
        addSidebarCounts(model);

        return "super-admin/admins";
    }

    @PostMapping("/admins/promote")
    public String promoteToAdmin(@RequestParam String username,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            User performedBy = superAdminService.findUserByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Super Admin not found"));
            User target = superAdminService.findUserByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            superAdminService.promoteToAdmin(target.getId(), performedBy);
            redirectAttributes.addFlashAttribute("success", username + " has been promoted to Admin.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "Not authorized: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/admins";
    }

    @PostMapping("/admins/{id}/demote")
    public String demoteAdmin(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            User performedBy = superAdminService.findUserByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Super Admin not found"));
            superAdminService.demoteAdmin(id, performedBy);
            redirectAttributes.addFlashAttribute("success", "Admin demoted to regular user.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "Not authorized: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/admins";
    }

    // ==================== AUDIT LOGS ====================

    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(required = false) Long adminId,
                            @RequestParam(required = false) String action,
                            @RequestParam(required = false) String from,
                            @RequestParam(required = false) String to,
                            @RequestParam(defaultValue = "0") int page,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        // Parse optional date range (HTML date inputs give yyyy-MM-dd)
        java.time.LocalDateTime fromTime = null;
        java.time.LocalDateTime toTime = null;
        try {
            if (from != null && !from.isBlank()) {
                fromTime = java.time.LocalDate.parse(from).atStartOfDay();
            }
            if (to != null && !to.isBlank()) {
                toTime = java.time.LocalDate.parse(to).atTime(23, 59, 59);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Invalid date filter — showing unfiltered logs.");
        }

        int pageSize = 50;
        org.springframework.data.domain.Page<AdminAuditLog> logPage = auditService.getFilteredLogs(
                adminId, action, fromTime, toTime, org.springframework.data.domain.PageRequest.of(page, pageSize));

        model.addAttribute("auditLogs", logPage.getContent());
        model.addAttribute("totalLogs", logPage.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logPage.getTotalPages());

        // Filter dropdown data + selected values (to restore form state)
        model.addAttribute("admins", auditService.getDistinctPerformers());
        model.addAttribute("actions", auditService.getDistinctActions());
        model.addAttribute("selectedAdminId", adminId);
        model.addAttribute("selectedAction", action);
        model.addAttribute("selectedFrom", from);
        model.addAttribute("selectedTo", to);

        // Pretty labels for each action type
        java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
        labels.put("USER_CREATED", "User Created");
        labels.put("USER_DELETED", "User Deleted");
        labels.put("USER_ACTIVATED", "User Activated");
        labels.put("USER_DEACTIVATED", "User Deactivated");
        labels.put("USER_BULK_ACTION", "User Bulk Action");
        labels.put("ROLE_CHANGED", "Role Changed");
        labels.put("ADMIN_CREATED", "Admin Promoted");
        labels.put("ADMIN_DEMOTED", "Admin Demoted");
        labels.put("BALANCE_ADJUSTED", "Balance Adjusted");
        labels.put("AUCTION_APPROVED", "Auction Approved");
        labels.put("AUCTION_REJECTED", "Auction Rejected");
        labels.put("AUCTION_CLOSED", "Auction Closed");
        labels.put("AUCTION_REOPENED", "Auction Reopened");
        labels.put("AUCTION_EXTENDED", "Auction Extended");
        labels.put("AUCTION_CANCELLED", "Auction Cancelled");
        labels.put("AUCTION_DELETED", "Auction Deleted");
        labels.put("AUCTION_EDITED", "Auction Edited");
        labels.put("AUCTION_BULK_ACTION", "Auction Bulk Action");
        labels.put("BROADCAST_SENT", "Broadcast Sent");
        labels.put("BROADCAST_SCHEDULED", "Broadcast Scheduled");
        labels.put("BROADCAST_CANCELLED", "Broadcast Cancelled");
        labels.put("BROADCAST_RESCHEDULED", "Broadcast Rescheduled");
        labels.put("SLIDE_CREATED", "Slide Created");
        labels.put("SLIDE_UPDATED", "Slide Updated");
        labels.put("SLIDE_DELETED", "Slide Deleted");
        labels.put("SLIDE_ACTIVATED", "Slide Activated");
        labels.put("SLIDE_DEACTIVATED", "Slide Deactivated");
        labels.put("PAYMENT_VERIFIED", "Payment Verified");
        labels.put("PAYMENT_RELEASED", "Payment Released");
        labels.put("PAYMENT_REJECTED", "Payment Rejected");
        labels.put("TICKET_REPLIED", "Ticket Replied");
        labels.put("TICKET_STATUS_CHANGED", "Ticket Status Changed");
        labels.put("SETTINGS_UPDATED", "Settings Updated");
        model.addAttribute("actionLabels", labels);

        model.addAttribute("currentSuperAdmin", superAdminService.findUserByUsername(userDetails.getUsername()).orElse(null));
        addSidebarCounts(model);
        return "super-admin/audit-logs";
    }

    // ==================== API: REAL-TIME VALIDATION ====================

    @GetMapping("/api/check-username")
    @ResponseBody
    public java.util.Map<String, Object> checkUsername(@RequestParam String username,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        String trimmed = username != null ? username.trim() : "";

        if (trimmed.length() < 3 || trimmed.length() > 30) {
            result.put("available", false);
            result.put("message", "Username must be 3–30 characters");
            return result;
        }

        if (userDetails != null && trimmed.equals(userDetails.getUsername())) {
            result.put("available", true);
            result.put("message", "Current username");
            return result;
        }

        boolean taken = superAdminService.findUserByUsername(trimmed).isPresent();
        result.put("available", !taken);
        result.put("message", taken ? "Username is taken" : "Username is available");
        return result;
    }

    @GetMapping("/api/check-email")
    @ResponseBody
    public java.util.Map<String, Object> checkEmail(@RequestParam String email,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        String trimmed = email != null ? email.trim() : "";

        if (!trimmed.contains("@") || trimmed.length() > 100) {
            result.put("available", false);
            result.put("message", "Enter a valid email");
            return result;
        }

        if (userDetails != null) {
            User currentUser = superAdminService.findUserByUsername(userDetails.getUsername()).orElse(null);
            if (currentUser != null && trimmed.equals(currentUser.getEmail())) {
                result.put("available", true);
                result.put("message", "Current email");
                return result;
            }
        }

        boolean taken = superAdminService.findUserByUsername(trimmed).isPresent();
        // Also check by email specifically
        boolean emailTaken = superAdminService.getAllUsers().stream()
                .anyMatch(u -> trimmed.equalsIgnoreCase(u.getEmail()));
        result.put("available", !emailTaken);
        result.put("message", emailTaken ? "Email is already in use" : "Email is available");
        return result;
    }
}
