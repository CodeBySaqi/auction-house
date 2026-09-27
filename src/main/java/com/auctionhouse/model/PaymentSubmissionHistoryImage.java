package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Image belonging to a historical submission snapshot.
 */
@Entity
@Table(name = "payment_submission_history_images")
public class PaymentSubmissionHistoryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private PaymentSubmissionHistory history;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PaymentSubmissionHistoryImage() {
        this.createdAt = LocalDateTime.now();
    }

    public PaymentSubmissionHistoryImage(PaymentSubmissionHistory history, String url, int sortOrder) {
        this.history = history;
        this.url = url;
        this.sortOrder = sortOrder;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PaymentSubmissionHistory getHistory() { return history; }
    public void setHistory(PaymentSubmissionHistory history) { this.history = history; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
