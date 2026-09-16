package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Auction entity - base class for all auction types.
 * Demonstrates Inheritance: subclasses extend this with specific properties.
 * Demonstrates Abstraction: abstract methods define contract for subclasses.
 */
@Entity
@Table(name = "auctions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "auction_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "starting_price", nullable = false)
    private double startingPrice;

    @Column(name = "current_highest_bid")
    private double currentHighestBid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "highest_bidder_id")
    private User highestBidder;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private AuctionCategory category;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC")
    private List<Bid> bids = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "bid_count")
    private int bidCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Approval system fields
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public Auction() {
        this.createdAt = LocalDateTime.now();
        this.currentHighestBid = 0;
        this.status = AuctionStatus.PENDING_APPROVAL; // Default to pending approval
    }

    /**
     * Abstract method - each auction type provides its own details.
     * Demonstrates Polymorphism.
     */
    public abstract String getCategoryDetails();

    /**
     * Abstract method - each auction type returns its type name.
     */
    public abstract String getTypeName();

    /**
     * Accept a bid on this auction. Validates and updates state.
     * Demonstrates Encapsulation: internal state management.
     */
    public boolean acceptBid(Bid bid) {
        if (this.status != AuctionStatus.ACTIVE) {
            return false;
        }
        if (isExpired()) {
            this.status = AuctionStatus.CLOSED;
            return false;
        }
        if (bid.getAmount() <= this.currentHighestBid) {
            return false;
        }
        if (bid.getAmount() < this.startingPrice) {
            return false;
        }

        this.currentHighestBid = bid.getAmount();
        this.highestBidder = bid.getBidder();
        this.bidCount++;
        this.bids.add(bid);
        return true;
    }

    /**
     * Close the auction and determine the winner.
     */
    public User closeAuction() {
        this.status = AuctionStatus.CLOSED;
        return this.highestBidder;
    }

    /**
     * Check if the auction has expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.endTime);
    }

    /**
     * Get the minimum acceptable bid (current highest + minimum increment).
     * Allows $1 minimum increment for user-friendly bidding.
     */
    public double getMinimumBid() {
        if (this.currentHighestBid == 0) {
            return this.startingPrice;
        }
        // Allow minimum $1 increment for better user experience
        double increment = Math.max(1.0, Math.ceil(this.currentHighestBid * 0.01));
        return this.currentHighestBid + increment;
    }

    /**
     * Get time remaining as a formatted string.
     */
    public String getTimeRemaining() {
        if (isExpired()) return "Ended";
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(now, endTime);
        long hours = ChronoUnit.HOURS.between(now, endTime) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, endTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, endTime) % 60;

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    public long getTimeRemainingSeconds() {
        if (isExpired()) return 0;
        return ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
    }

    /**
     * Get winner (highest bidder when closed).
     */
    public User getWinner() {
        if (this.status == AuctionStatus.CLOSED) {
            return this.highestBidder;
        }
        return null;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public User getHighestBidder() { return highestBidder; }
    public void setHighestBidder(User highestBidder) { this.highestBidder = highestBidder; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public AuctionCategory getCategory() { return category; }
    public void setCategory(AuctionCategory category) { this.category = category; }

    public List<Bid> getBids() { return bids; }
    public void setBids(List<Bid> bids) { this.bids = bids; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getBidCount() { return bidCount; }
    public void setBidCount(int bidCount) { this.bidCount = bidCount; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    /**
     * Get display price - either current highest bid or starting price.
     */
    public double getDisplayPrice() {
        return currentHighestBid > 0 ? currentHighestBid : startingPrice;
    }
}
