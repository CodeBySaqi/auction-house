package com.auctionhouse.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentRelease entity - tracks buyer/seller verification and admin payment release.
 * Created when an auction is won, tracks delivery details from both parties,
 * and manages admin verification before releasing payment to seller.
 */
@Entity
@Table(name = "payment_releases")
public class PaymentRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false, unique = true)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "winning_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal winningAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.WAITING_FOR_DETAILS;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Seller Details
    @Column(name = "seller_shipping_method", length = 200)
    private String sellerShippingMethod;

    @Column(name = "seller_tracking_number", length = 100)
    private String sellerTrackingNumber;

    @Column(name = "seller_courier_name", length = 100)
    private String sellerCourierName;

    @Column(name = "seller_shipment_date")
    private LocalDateTime sellerShipmentDate;

    @Column(name = "seller_proof_path", length = 500)
    private String sellerProofPath;

    @Column(name = "seller_note", length = 1000)
    private String sellerNote;

    @Column(name = "seller_submitted_at")
    private LocalDateTime sellerSubmittedAt;

    // Buyer Details
    @Column(name = "buyer_received_confirmation")
    private Boolean buyerReceivedConfirmation;

    @Column(name = "buyer_received_date")
    private LocalDateTime buyerReceivedDate;

    @Column(name = "buyer_proof_path", length = 500)
    private String buyerProofPath;

    @Column(name = "buyer_note", length = 1000)
    private String buyerNote;

    @Column(name = "buyer_submitted_at")
    private LocalDateTime buyerSubmittedAt;

    // Admin Verification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    // Payment Release
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    private User releasedBy;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "payment_released")
    private Boolean paymentReleased = false;

    public PaymentRelease() {
        this.createdAt = LocalDateTime.now();
    }

    public PaymentRelease(Auction auction, User buyer, User seller, BigDecimal winningAmount) {
        this.auction = auction;
        this.buyer = buyer;
        this.seller = seller;
        this.winningAmount = winningAmount;
        this.status = VerificationStatus.WAITING_FOR_DETAILS;
        this.createdAt = LocalDateTime.now();
        this.paymentReleased = false;
    }

    // Helper methods
    public boolean isSellerDetailsSubmitted() {
        return sellerSubmittedAt != null;
    }

    public boolean isBuyerDetailsSubmitted() {
        return buyerSubmittedAt != null;
    }

    public boolean isReadyForReview() {
        return status == VerificationStatus.READY_FOR_ADMIN_REVIEW;
    }

    public boolean isPaymentReleased() {
        return paymentReleased != null && paymentReleased;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }

    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public BigDecimal getWinningAmount() { return winningAmount; }
    public void setWinningAmount(BigDecimal winningAmount) { this.winningAmount = winningAmount; }

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSellerShippingMethod() { return sellerShippingMethod; }
    public void setSellerShippingMethod(String sellerShippingMethod) { this.sellerShippingMethod = sellerShippingMethod; }

    public String getSellerTrackingNumber() { return sellerTrackingNumber; }
    public void setSellerTrackingNumber(String sellerTrackingNumber) { this.sellerTrackingNumber = sellerTrackingNumber; }

    public String getSellerCourierName() { return sellerCourierName; }
    public void setSellerCourierName(String sellerCourierName) { this.sellerCourierName = sellerCourierName; }

    public LocalDateTime getSellerShipmentDate() { return sellerShipmentDate; }
    public void setSellerShipmentDate(LocalDateTime sellerShipmentDate) { this.sellerShipmentDate = sellerShipmentDate; }

    public String getSellerProofPath() { return sellerProofPath; }
    public void setSellerProofPath(String sellerProofPath) { this.sellerProofPath = sellerProofPath; }

    public String getSellerNote() { return sellerNote; }
    public void setSellerNote(String sellerNote) { this.sellerNote = sellerNote; }

    public LocalDateTime getSellerSubmittedAt() { return sellerSubmittedAt; }
    public void setSellerSubmittedAt(LocalDateTime sellerSubmittedAt) { this.sellerSubmittedAt = sellerSubmittedAt; }

    public Boolean getBuyerReceivedConfirmation() { return buyerReceivedConfirmation; }
    public void setBuyerReceivedConfirmation(Boolean buyerReceivedConfirmation) { this.buyerReceivedConfirmation = buyerReceivedConfirmation; }

    public LocalDateTime getBuyerReceivedDate() { return buyerReceivedDate; }
    public void setBuyerReceivedDate(LocalDateTime buyerReceivedDate) { this.buyerReceivedDate = buyerReceivedDate; }

    public String getBuyerProofPath() { return buyerProofPath; }
    public void setBuyerProofPath(String buyerProofPath) { this.buyerProofPath = buyerProofPath; }

    public String getBuyerNote() { return buyerNote; }
    public void setBuyerNote(String buyerNote) { this.buyerNote = buyerNote; }

    public LocalDateTime getBuyerSubmittedAt() { return buyerSubmittedAt; }
    public void setBuyerSubmittedAt(LocalDateTime buyerSubmittedAt) { this.buyerSubmittedAt = buyerSubmittedAt; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public User getReleasedBy() { return releasedBy; }
    public void setReleasedBy(User releasedBy) { this.releasedBy = releasedBy; }

    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }

    public Boolean getPaymentReleased() { return paymentReleased; }
    public void setPaymentReleased(Boolean paymentReleased) { this.paymentReleased = paymentReleased; }
}
