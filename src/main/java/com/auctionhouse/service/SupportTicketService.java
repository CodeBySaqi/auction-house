package com.auctionhouse.service;

import com.auctionhouse.model.*;
import com.auctionhouse.repository.SupportTicketRepository;
import com.auctionhouse.repository.TicketMessageRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SupportTicketService - manages support tickets and their conversations.
 * Handles creation, messaging, status transitions, and access control.
 */
@Service
public class SupportTicketService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_SUBJECT_LENGTH = 200;

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public SupportTicketService(SupportTicketRepository ticketRepository,
                                 TicketMessageRepository messageRepository,
                                 UserRepository userRepository,
                                 NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Create a new support ticket with initial message.
     */
    @Transactional
    public SupportTicket createTicket(User user, String subject, TicketCategory category, String message) {
        // Validate inputs
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        if (subject.length() > MAX_SUBJECT_LENGTH) {
            throw new IllegalArgumentException("Subject too long (max " + MAX_SUBJECT_LENGTH + " characters).");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long (max " + MAX_MESSAGE_LENGTH + " characters).");
        }

        // Create ticket
        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setSubject(subject.trim());
        ticket.setCategory(category);
        ticket.setPriority("MEDIUM"); // System-controlled
        ticket = ticketRepository.save(ticket);

        // Generate ticket number using ID
        ticket.setTicketNumber(SupportTicket.generateTicketNumber(ticket.getId()));
        ticket = ticketRepository.save(ticket);

        // Create initial message
        TicketMessage initialMessage = new TicketMessage(ticket, user, "USER", message.trim());
        messageRepository.save(initialMessage);

        // Notify admins about new ticket
        notifyAdminsNewTicket(ticket);

        return ticket;
    }

    /**
     * Send a message in a ticket conversation.
     * Validates ownership and ticket status.
     */
    @Transactional
    public TicketMessage sendMessage(Long ticketId, User sender, String content) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found."));

        // Validate content
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long (max " + MAX_MESSAGE_LENGTH + " characters).");
        }

        // Determine sender role and validate access
        boolean isOwner = ticket.getUser().getId().equals(sender.getId());
        boolean isAdmin = isAdmin(sender);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("You do not have access to this ticket.");
        }

        // Check if ticket is closed
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalArgumentException("This ticket is closed. No further messages can be sent.");
        }

        // If user sends message on RESOLVED ticket, reopen it
        if (isOwner && ticket.getStatus() == TicketStatus.RESOLVED) {
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setResolvedAt(null);
        }

        // Determine sender role
        String senderRole = isAdmin ? "ADMIN" : "USER";

        // Create message
        TicketMessage msg = new TicketMessage(ticket, sender, senderRole, content.trim());
        msg = messageRepository.save(msg);

        // Update ticket timestamp
        ticket.setUpdatedAt(LocalDateTime.now());

        // Auto-set to IN_PROGRESS if admin replies to OPEN ticket
        if (isAdmin && ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            if (ticket.getAssignedAdmin() == null) {
                ticket.setAssignedAdmin(sender);
            }
        }

        ticketRepository.save(ticket);

        // Mark previous messages as read for the sender
        messageRepository.markAsRead(ticketId, sender.getId());

        // Send notification
        if (isAdmin) {
            // Notify the ticket owner
            notificationService.createNotification(
                    ticket.getUser(),
                    "💬 Admin replied to your ticket \"" + ticket.getSubject() + "\"",
                    "SUPPORT",
                    null
            );
        } else {
            // Notify admins
            notifyAdminsNewReply(ticket, sender);
        }

        return msg;
    }

    /**
     * Get a ticket with ownership/admin validation.
     */
    public SupportTicket getTicket(Long ticketId, User requestingUser) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found."));

        boolean isOwner = ticket.getUser().getId().equals(requestingUser.getId());
        boolean isAdmin = isAdmin(requestingUser);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("You do not have access to this ticket.");
        }

        return ticket;
    }

    /**
     * Get tickets for a user.
     */
    public List<SupportTicket> getUserTickets(Long userId) {
        return ticketRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * Get messages for a ticket.
     */
    public List<TicketMessage> getMessages(Long ticketId) {
        return messageRepository.findByTicketId(ticketId);
    }

    /**
     * Mark messages as read for a user viewing a ticket.
     */
    @Transactional
    public void markMessagesRead(Long ticketId, Long userId) {
        messageRepository.markAsRead(ticketId, userId);
    }

    /**
     * Change ticket status (admin only).
     */
    @Transactional
    public void changeStatus(Long ticketId, User admin, TicketStatus newStatus) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found."));

        if (!isAdmin(admin)) {
            throw new SecurityException("Only admins can change ticket status.");
        }

        // Validate status transitions
        TicketStatus current = ticket.getStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new IllegalArgumentException("Cannot change status from " + current.getDisplayName() + " to " + newStatus.getDisplayName());
        }

        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
            // Notify user
            notificationService.createNotification(
                    ticket.getUser(),
                    "✅ Your ticket \"" + ticket.getSubject() + "\" has been resolved.",
                    "SUPPORT",
                    null
            );
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
            ticket.setClosedBy(admin);
            // Notify user
            notificationService.createNotification(
                    ticket.getUser(),
                    "🔒 Your ticket \"" + ticket.getSubject() + "\" has been closed.",
                    "SUPPORT",
                    null
            );
        }

        ticketRepository.save(ticket);
    }

    /**
     * Admin: get all tickets with pagination and optional status filter.
     */
    public Page<SupportTicket> getAdminTickets(TicketStatus status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (search != null && !search.trim().isEmpty()) {
            return ticketRepository.searchTickets(search.trim(), pageable);
        }

        if (status != null) {
            return ticketRepository.findByStatus(status, pageable);
        }

        return ticketRepository.findAllOrderByUpdatedAtDesc(pageable);
    }

    /**
     * Count open (non-closed) tickets for admin badge.
     */
    public long countOpenTickets() {
        return ticketRepository.countOpenTickets();
    }

    /**
     * Count unread messages for a user across all their tickets.
     */
    public int countTotalUnread(Long userId) {
        List<SupportTicket> tickets = ticketRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        int total = 0;
        for (SupportTicket t : tickets) {
            total += t.getUnreadCountForUser(userId);
        }
        return total;
    }

    // --- Helpers ---

    private boolean isAdmin(User user) {
        return "ROLE_ADMIN".equals(user.getRole()) || "ROLE_SUPER_ADMIN".equals(user.getRole());
    }

    private boolean isValidTransition(TicketStatus from, TicketStatus to) {
        if (from == to) return false;
        switch (from) {
            case OPEN: return to == TicketStatus.IN_PROGRESS || to == TicketStatus.CLOSED;
            case IN_PROGRESS: return to == TicketStatus.RESOLVED || to == TicketStatus.CLOSED || to == TicketStatus.OPEN;
            case RESOLVED: return to == TicketStatus.OPEN || to == TicketStatus.CLOSED;
            case CLOSED: return false; // Cannot change from closed
            default: return false;
        }
    }

    private void notifyAdminsNewTicket(SupportTicket ticket) {
        List<User> admins = userRepository.findByRole("ROLE_ADMIN");
        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    "📋 New support ticket " + ticket.getTicketNumber() + ": \"" + ticket.getSubject() + "\" from " + ticket.getUser().getUsername(),
                    "SUPPORT",
                    null
            );
        }
    }

    private void notifyAdminsNewReply(SupportTicket ticket, User sender) {
        // Notify assigned admin, or all admins if unassigned
        if (ticket.getAssignedAdmin() != null) {
            notificationService.createNotification(
                    ticket.getAssignedAdmin(),
                    "💬 New reply on ticket " + ticket.getTicketNumber() + ": \"" + ticket.getSubject() + "\"",
                    "SUPPORT",
                    null
            );
        } else {
            List<User> admins = userRepository.findByRole("ROLE_ADMIN");
            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "💬 New reply on ticket " + ticket.getTicketNumber() + ": \"" + ticket.getSubject() + "\"",
                        "SUPPORT",
                        null
                );
            }
        }
    }
}
