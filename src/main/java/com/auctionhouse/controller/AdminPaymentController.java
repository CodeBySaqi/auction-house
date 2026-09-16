package com.auctionhouse.controller;

import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.User;
import com.auctionhouse.model.VerificationStatus;
import com.auctionhouse.service.PaymentReleaseService;
import com.auctionhouse.service.UserService;
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
 * AdminPaymentController - handles admin verification and payment release.
 */
@Controller
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentReleaseService paymentReleaseService;
    private final UserService userService;

    @Autowired
    public AdminPaymentController(PaymentReleaseService paymentReleaseService, UserService userService) {
        this.paymentReleaseService = paymentReleaseService;
        this.userService = userService;
    }

    /**
     * List all payment releases.
     */
    @GetMapping
    public String listPaymentReleases(@RequestParam(required = false) String filter,
                                       Model model) {
        List<PaymentRelease> paymentReleases;
        
        if ("pending".equals(filter)) {
            paymentReleases = paymentReleaseService.getPendingAdminReview();
        } else if ("released".equals(filter)) {
            paymentReleases = paymentReleaseService.getReleasedPayments();
        } else {
            // All payment releases
            paymentReleases = paymentReleaseService.getAllPaymentReleases();
        }

        model.addAttribute("paymentReleases", paymentReleases);
        model.addAttribute("currentFilter", filter != null ? filter : "all");
        model.addAttribute("pendingCount", paymentReleaseService.countPendingReview());
        
        return "admin/payment-releases";
    }

    /**
     * View details of a specific payment release.
     */
    @GetMapping("/{id}")
    public String viewPaymentRelease(@PathVariable Long id,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            PaymentRelease pr = paymentReleaseService.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment release not found"));

            model.addAttribute("paymentRelease", pr);
            model.addAttribute("auction", pr.getAuction());
            model.addAttribute("buyer", pr.getBuyer());
            model.addAttribute("seller", pr.getSeller());
            
            return "admin/payment-release-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/payments";
        }
    }

    /**
     * Verify and approve a payment release.
     */
    @PostMapping("/{id}/verify")
    public String verifyPaymentRelease(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            paymentReleaseService.verifyPaymentRelease(id, admin);

            redirectAttributes.addFlashAttribute("success", "Payment release verified and approved successfully!");
            return "redirect:/admin/payments/" + id;
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to perform this action.");
            return "redirect:/admin/payments/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to verify: " + e.getMessage());
            return "redirect:/admin/payments/" + id;
        }
    }

    /**
     * Release payment to seller.
     */
    @PostMapping("/{id}/release")
    public String releasePayment(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            paymentReleaseService.releasePayment(id, admin);

            redirectAttributes.addFlashAttribute("success", "Payment released to seller successfully!");
            return "redirect:/admin/payments/" + id;
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to perform this action.");
            return "redirect:/admin/payments/" + id;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/payments/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to release payment: " + e.getMessage());
            return "redirect:/admin/payments/" + id;
        }
    }

    /**
     * Reject a payment release.
     */
    @PostMapping("/{id}/reject")
    public String rejectPaymentRelease(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam String reason,
                                        RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            paymentReleaseService.rejectPaymentRelease(id, admin, reason);

            redirectAttributes.addFlashAttribute("success", "Payment release rejected. Both parties notified to resubmit.");
            return "redirect:/admin/payments/" + id;
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to perform this action.");
            return "redirect:/admin/payments/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject: " + e.getMessage());
            return "redirect:/admin/payments/" + id;
        }
    }
}
