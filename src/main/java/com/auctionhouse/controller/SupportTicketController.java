package com.auctionhouse.controller;

import com.auctionhouse.model.*;
import com.auctionhouse.service.SupportTicketService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * SupportTicketController - user-side support ticket management.
 * Handles ticket creation, listing, and conversation viewing.
 */
@Controller
public class SupportTicketController {

    private final SupportTicketService ticketService;
    private final UserService userService;

    @Autowired
    public SupportTicketController(SupportTicketService ticketService, UserService userService) {
        this.ticketService = ticketService;
        this.userService = userService;
    }

    /**
     * GET /support - My Tickets list
     */
    @GetMapping("/support")
    public String myTickets(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<SupportTicket> tickets = ticketService.getUserTickets(user.getId());
        int unreadTotal = ticketService.countTotalUnread(user.getId());

        model.addAttribute("tickets", tickets);
        model.addAttribute("unreadTotal", unreadTotal);
        model.addAttribute("openTicketCount", tickets.stream()
                .filter(t -> t.getStatus() != TicketStatus.CLOSED).count());
        return "profile/support-tickets";
    }

    /**
     * GET /support/create - Create ticket form
     */
    @GetMapping("/support/create")
    public String createTicketForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<SupportTicket> openTickets = ticketService.getUserTickets(user.getId());
        long openCount = openTickets.stream()
                .filter(t -> t.getStatus() != TicketStatus.CLOSED && t.getStatus() != TicketStatus.RESOLVED)
                .count();

        model.addAttribute("categories", TicketCategory.values());
        model.addAttribute("openTicketCount", openCount);
        return "profile/support-create";
    }

    /**
     * POST /support/create - Submit new ticket
     */
    @PostMapping("/support/create")
    public String createTicket(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam String subject,
                                @RequestParam String category,
                                @RequestParam String message,
                                RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(userDetails.getUsername());

        try {
            TicketCategory cat = TicketCategory.valueOf(category);
            SupportTicket ticket = ticketService.createTicket(user, subject, cat, message);
            redirectAttributes.addFlashAttribute("success", "Ticket " + ticket.getTicketNumber() + " created successfully!");
            return "redirect:/support/" + ticket.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("subject", subject);
            redirectAttributes.addFlashAttribute("category", category);
            redirectAttributes.addFlashAttribute("message", message);
            return "redirect:/support/create";
        }
    }

    /**
     * GET /support/{id} - View ticket conversation
     */
    @GetMapping("/support/{id}")
    public String viewTicket(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());

        try {
            SupportTicket ticket = ticketService.getTicket(id, user);
            List<TicketMessage> messages = ticketService.getMessages(id);

            // Mark messages as read
            ticketService.markMessagesRead(id, user.getId());

            model.addAttribute("ticket", ticket);
            model.addAttribute("messages", messages);
            model.addAttribute("isOwner", ticket.getUser().getId().equals(user.getId()));
            return "profile/support-conversation";
        } catch (SecurityException e) {
            return "redirect:/support";
        } catch (IllegalArgumentException e) {
            return "redirect:/support";
        }
    }

    /**
     * POST /support/{id}/reply - Send message (fallback for non-JS)
     */
    @PostMapping("/support/{id}/reply")
    public String replyTicket(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String content,
                               RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(userDetails.getUsername());

        try {
            ticketService.sendMessage(id, user, content);
            return "redirect:/support/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/support/" + id;
        }
    }


}
