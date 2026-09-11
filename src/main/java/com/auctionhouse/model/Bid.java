package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bid entity - represents a single bid placed by a user on an auction.
 */
@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_winning")
    private boolean isWinning = true;

    public Bid() {
        this.timestamp = LocalDateTime.now();
    }

    public Bid(User bidder, Auction auction, double amount) {
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.isWinning = true;
    }

    /**
     * Check if this bid is valid (above minimum).
     */
    public boolean isValid() {
        return amount >= auction.getMinimumBid() && amount >= auction.getStartingPrice();
    }

    /**
     * Format the timestamp for display.
     */
    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));
    }

    public String getTimeAgo() {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(timestamp, now);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }

    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isWinning() { return isWinning; }
    public void setWinning(boolean winning) { isWinning = winning; }
}
