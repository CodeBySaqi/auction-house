package com.auctionhouse.controller;

import com.auctionhouse.model.PlatformCommission;
import com.auctionhouse.model.Auction;
import com.auctionhouse.service.AuctionService;
import com.auctionhouse.service.PaymentReleaseService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * AdminCommissionController - Platform Commission dashboard.
 * Only accessible by ROLE_ADMIN.
 */
@Controller
@RequestMapping("/admin/commissions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionController {

    private final PaymentReleaseService paymentReleaseService;
    private final AuctionService auctionService;
    private final UserService userService;

    @Autowired
    public AdminCommissionController(PaymentReleaseService paymentReleaseService, AuctionService auctionService, UserService userService) {
        this.paymentReleaseService = paymentReleaseService;
        this.auctionService = auctionService;
        this.userService = userService;
    }

    /**
     * Platform Commission dashboard showing all commission records and totals.
     */
    @GetMapping
    public String commissions(Model model) {
        List<PlatformCommission> commissions = paymentReleaseService.getAllCommissions();

        model.addAttribute("commissions", commissions);
        model.addAttribute("totalCommission", paymentReleaseService.getTotalCommission());
        model.addAttribute("totalSellerPayout", paymentReleaseService.getTotalSellerPayout());
        model.addAttribute("totalWinningAmount", paymentReleaseService.getTotalWinningAmount());
        model.addAttribute("commissionCount", paymentReleaseService.getCommissionCount());
        model.addAttribute("commissionRate", PlatformCommission.COMMISSION_RATE);

        addSidebarAttributes(model);
        return "admin/commissions";
    }

    private void addSidebarAttributes(Model model) {
        model.addAttribute("totalAuctions", auctionService.getAllAuctions().size());
        model.addAttribute("pendingCount", auctionService.getPendingAuctions().size());
        model.addAttribute("pendingPaymentCount", paymentReleaseService.countPendingReview());
        model.addAttribute("totalUsers", userService.findAllUsers().size());
    }
}
