package com.auctionhouse.controller;

import com.auctionhouse.model.*;
import com.auctionhouse.service.SupportTicketService;
import com.auctionhouse.service.UserService;
import com.auctionhouse.service.NotificationService;
import com.auctionhouse.service.AuctionService;
import com.auctionhouse.service.PaymentReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * AdminSupportController - admin-side support ticket management.
 * Integrated into the existing admin dashboard with sidebar navigation.
 */
@Controller
@RequestMapping("/admin/support")
public class AdminSupportController {

    private final SupportTicketService ticketService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuctionService auctionService;
    private final PaymentReleaseService paymentReleaseService;

    @Autowired
    public AdminSupportController(SupportTicketService ticketService,
                                   UserService userService,
                                   NotificationService notificationService,
                                   AuctionService auctionService,
                                   PaymentReleaseService paymentReleaseService) {
        this.ticketService = ticketService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.auctionService = auctionService;
        this.paymentReleaseService = paymentReleaseService;
    }

    /**
     * GET /admin/support - List all tickets with filters
     */
    @GetMapping
    public String listTickets(Model model, Principal principal,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String search,
                               @RequestParam(defaultValue = "0") int page) {
        User admin = requireAdmin(principal, model);

        TicketStatus statusFilter = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusFilter = TicketStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {}
        }

        Page<SupportTicket> tickets = ticketService.getAdminTickets(statusFilter, search, page, 20);

        model.addAttribute("tickets", tickets.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tickets.getTotalPages());
        model.addAttribute("totalTickets", tickets.getTotalElements());
        model.addAttribute("currentStatus", status != null ? status : "");
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("statuses", TicketStatus.values());

        // Stats
        model.addAttribute("openCount", ticketService.countOpenTickets());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));

        addSidebarAttributes(model);
        return "admin/support-tickets";
    }

    /**
     * GET /admin/support/{id} - View ticket conversation
     */
    @GetMapping("/{id}")
    public String viewTicket(@PathVariable Long id, Model model, Principal principal) {
        User admin = requireAdmin(principal, model);

        try {
            SupportTicket ticket = ticketService.getTicket(id, admin);
            List<TicketMessage> messages = ticketService.getMessages(id);

            // Mark messages as read for admin
            ticketService.markMessagesRead(id, admin.getId());

            model.addAttribute("ticket", ticket);
            model.addAttribute("messages", messages);
            model.addAttribute("statuses", TicketStatus.values());
            model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));

            addSidebarAttributes(model);
            return "admin/support-conversation";
        } catch (Exception e) {
            return "redirect:/admin/support";
        }
    }

    /**
     * POST /admin/support/{id}/reply - Admin reply (fallback for non-JS)
     */
    @PostMapping("/{id}/reply")
    public String replyTicket(@PathVariable Long id, Principal principal,
                               @RequestParam String content,
                               RedirectAttributes redirectAttributes) {
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        try {
            ticketService.sendMessage(id, admin, content);
            return "redirect:/admin/support/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/support/" + id;
        }
    }

    /**
     * POST /admin/support/{id}/status - Change ticket status
     */
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, Principal principal,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        try {
            TicketStatus newStatus = TicketStatus.valueOf(status);
            ticketService.changeStatus(id, admin, newStatus);
            redirectAttributes.addFlashAttribute("success", "Ticket status updated to " + newStatus.getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/support/" + id;
    }

    // --- Helpers ---

    private User requireAdmin(Principal principal, Model model) {
        User admin = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        model.addAttribute("admin", admin);
        return admin;
    }

    private void addSidebarAttributes(Model model) {
        List<Auction> allAuctions = auctionService.getAllAuctions();
        model.addAttribute("totalAuctions", allAuctions.size());
        model.addAttribute("pendingCount", auctionService.getPendingAuctions().size());
        model.addAttribute("pendingPaymentCount", paymentReleaseService.countPendingReview());
        model.addAttribute("totalUsers", userService.findAllUsers().size());
    }
}
