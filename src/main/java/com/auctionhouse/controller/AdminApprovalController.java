package com.auctionhouse.controller;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.User;
import com.auctionhouse.service.AuctionService;
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
 * AdminApprovalController - handles auction approval/rejection workflow.
 * Only accessible by ADMIN or SUPER_ADMIN roles.
 */
@Controller
@RequestMapping("/admin/approvals")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminApprovalController {

    private final AuctionService auctionService;
    private final UserService userService;
    private final PaymentReleaseService paymentReleaseService;

    @Autowired
    public AdminApprovalController(AuctionService auctionService, UserService userService, PaymentReleaseService paymentReleaseService) {
        this.auctionService = auctionService;
        this.userService = userService;
        this.paymentReleaseService = paymentReleaseService;
    }

    /**
     * Display all pending auctions awaiting approval.
     */
    @GetMapping
    public String listPendingAuctions(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        List<Auction> pendingAuctions = auctionService.getPendingAuctions();
        model.addAttribute("pendingAuctions", pendingAuctions);
        model.addAttribute("currentUser", userService.getCurrentUser(userDetails.getUsername()));
        addSidebarAttributes(model);
        return "admin/approvals";
    }

    /**
     * View details of a specific auction for approval.
     */
    @GetMapping("/{id}")
    public String viewAuctionForApproval(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Auction auction = auctionService.findById(id)
            .orElseThrow(() -> new RuntimeException("Auction not found"));
        
        User currentUser = userService.getCurrentUser(userDetails.getUsername());
        
        // Prevent seller from approving their own auction
        boolean isOwner = auction.getCreatedBy() != null && 
                         auction.getCreatedBy().getId().equals(currentUser.getId());
        
        model.addAttribute("auction", auction);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwner", isOwner);
        return "admin/approval-detail";
    }

    /**
     * Approve an auction.
     */
    @PostMapping("/{id}/approve")
    public String approveAuction(@PathVariable Long id, 
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            Auction auction = auctionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
            
            User admin = userService.getCurrentUser(userDetails.getUsername());
            
            // Prevent seller from approving their own auction
            if (auction.getCreatedBy() != null && 
                auction.getCreatedBy().getId().equals(admin.getId())) {
                redirectAttributes.addFlashAttribute("error", "You cannot approve your own auction.");
                return "redirect:/admin/approvals/" + id;
            }
            
            auctionService.approveAuction(auction, admin);
            redirectAttributes.addFlashAttribute("success", "Auction \"" + auction.getTitle() + "\" has been approved and is now live!");
            return "redirect:/admin/approvals";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve auction: " + e.getMessage());
            return "redirect:/admin/approvals/" + id;
        }
    }

    /**
     * Reject an auction with a reason.
     */
    @PostMapping("/{id}/reject")
    public String rejectAuction(@PathVariable Long id,
                                 @RequestParam String reason,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Auction auction = auctionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
            
            User admin = userService.getCurrentUser(userDetails.getUsername());
            
            // Prevent seller from rejecting their own auction
            if (auction.getCreatedBy() != null && 
                auction.getCreatedBy().getId().equals(admin.getId())) {
                redirectAttributes.addFlashAttribute("error", "You cannot reject your own auction.");
                return "redirect:/admin/approvals/" + id;
            }
            
            if (reason == null || reason.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Rejection reason is required.");
                return "redirect:/admin/approvals/" + id;
            }
            
            auctionService.rejectAuction(auction, admin, reason.trim());
            redirectAttributes.addFlashAttribute("success", "Auction \"" + auction.getTitle() + "\" has been rejected.");
            return "redirect:/admin/approvals";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject auction: " + e.getMessage());
            return "redirect:/admin/approvals/" + id;
        }
    }

    private void addSidebarAttributes(Model model) {
        model.addAttribute("totalAuctions", auctionService.getAllAuctions().size());
        model.addAttribute("pendingCount", auctionService.getPendingAuctions().size());
        model.addAttribute("pendingPaymentCount", paymentReleaseService.countPendingReview());
        model.addAttribute("totalUsers", userService.findAllUsers().size());
    }
}
