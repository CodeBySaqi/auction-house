package com.auctionhouse.controller;

import com.auctionhouse.model.AdminAuditLog;
import com.auctionhouse.model.User;
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

    @Autowired
    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
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
    public String auditLogs(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("auditLogs", superAdminService.getAuditLogs());
        model.addAttribute("currentSuperAdmin", superAdminService.findUserByUsername(userDetails.getUsername()).orElse(null));
        return "super-admin/audit-logs";
    }
}
