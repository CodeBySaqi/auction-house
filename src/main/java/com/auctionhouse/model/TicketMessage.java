package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TicketMessage entity - a single message in a support ticket conversation.
 * Reuses the User model for sender identification.
 */
@Entity
@Table(name = "ticket_messages", indexes = {
    @Index(name = "idx_ticket_msg_ticket", columnList = "ticket_id"),
    @Index(name = "idx_ticket_msg_created", columnList = "created_at")
})
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole; // "USER" or "ADMIN"

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "is_read")
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TicketMessage() {
        this.createdAt = LocalDateTime.now();
    }

    public TicketMessage(SupportTicket ticket, User sender, String senderRole, String content) {
        this.ticket = ticket;
        this.sender = sender;
        this.senderRole = senderRole;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public String getFormattedTime() {
        return createdAt.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    public String getFormattedDateTime() {
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"));
    }

    public boolean isAdminMessage() {
        return "ADMIN".equals(senderRole);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SupportTicket getTicket() { return ticket; }
    public void setTicket(SupportTicket ticket) { this.ticket = ticket; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
