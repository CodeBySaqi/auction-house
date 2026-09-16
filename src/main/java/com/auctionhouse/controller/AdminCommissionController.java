package com.auctionhouse.controller;

import com.auctionhouse.model.PlatformCommission;
import com.auctionhouse.service.PaymentReleaseService;
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

    @Autowired
    public AdminCommissionController(PaymentReleaseService paymentReleaseService) {
        this.paymentReleaseService = paymentReleaseService;
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

        return "admin/commissions";
    }
}
