package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AdminAuditLog - records administrative actions for security and accountability.
 */
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_username", length = 50)
    private String targetUsername;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private User performedBy;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AdminAuditLog() {
        this.createdAt = LocalDateTime.now();
    }

    public AdminAuditLog(String action, Long targetUserId, String targetUsername, 
                         User performedBy, String details) {
        this.action = action;
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.performedBy = performedBy;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }

    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User performedBy) { this.performedBy = performedBy; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
