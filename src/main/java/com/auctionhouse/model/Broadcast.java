package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Broadcast entity - records each broadcast/announcement sent by admin.
 */
@Entity
@Table(name = "broadcasts")
public class Broadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "message_type")
    private String type; // announcement, alert, update, new_feature

    @Column(nullable = false)
    private String audience; // all, active, bidders, custom

    @Column(name = "recipient_count")
    private int recipientCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by")
    private User sentBy;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "is_scheduled")
    private boolean scheduled = false;

    @Column(name = "custom_user_ids", length = 2000)
    private String customUserIds; // comma-separated user IDs for custom audience

    public Broadcast() {
        this.sentAt = LocalDateTime.now();
    }

    public String getTimeAgo() {
        if (sentAt == null) return "Never";
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(sentAt, now);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 7) return days + "d ago";
        return sentAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    public String getFormattedScheduledTime() {
        if (scheduledFor == null) return "";
        return scheduledFor.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"));
    }

    public String getAudienceLabel() {
        switch (audience) {
            case "all": return "All Users (" + recipientCount + ")";
            case "active": return "Active Users (" + recipientCount + ")";
            case "bidders": return "Top Bidders (" + recipientCount + ")";
            case "custom": return "Selected Users (" + recipientCount + ")";
            default: return audience + " (" + recipientCount + ")";
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public int getRecipientCount() { return recipientCount; }
    public void setRecipientCount(int recipientCount) { this.recipientCount = recipientCount; }

    public User getSentBy() { return sentBy; }
    public void setSentBy(User sentBy) { this.sentBy = sentBy; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }

    public boolean isScheduled() { return scheduled; }
    public void setScheduled(boolean scheduled) { this.scheduled = scheduled; }

    public String getCustomUserIds() { return customUserIds; }
    public void setCustomUserIds(String customUserIds) { this.customUserIds = customUserIds; }
}
