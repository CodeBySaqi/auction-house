package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PaymentSubmissionHistory - keeps track of old submitted details
 * when admin requests correction. Allows admin to see what was submitted
 * before, even after seller/buyer resubmits.
 */
@Entity
@Table(name = "payment_submission_history")
public class PaymentSubmissionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_release_id", nullable = false)
    private PaymentRelease paymentRelease;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 20)
    private ProofType proofType; // SELLER or BUYER

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt; // when this version was originally submitted

    @Column(name = "correction_requested_at", nullable = false)
    private LocalDateTime correctionRequestedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    // Seller snapshot fields
    @Column(name = "seller_shipping_method", length = 200)
    private String sellerShippingMethod;

    @Column(name = "seller_tracking_number", length = 100)
    private String sellerTrackingNumber;

    @Column(name = "seller_courier_name", length = 100)
    private String sellerCourierName;

    @Column(name = "seller_shipment_date")
    private LocalDateTime sellerShipmentDate;

    @Column(name = "seller_note", length = 1000)
    private String sellerNote;

    @Column(name = "seller_proof_path", length = 500)
    private String sellerProofPath; // legacy first image

    // Buyer snapshot fields
    @Column(name = "buyer_received_confirmation")
    private Boolean buyerReceivedConfirmation;

    @Column(name = "buyer_received_date")
    private LocalDateTime buyerReceivedDate;

    @Column(name = "buyer_proof_path", length = 500)
    private String buyerProofPath;

    @Column(name = "buyer_note", length = 1000)
    private String buyerNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PaymentSubmissionHistoryImage> images = new ArrayList<>();

    public PaymentSubmissionHistory() {
        this.createdAt = LocalDateTime.now();
        this.correctionRequestedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PaymentRelease getPaymentRelease() { return paymentRelease; }
    public void setPaymentRelease(PaymentRelease paymentRelease) { this.paymentRelease = paymentRelease; }

    public ProofType getProofType() { return proofType; }
    public void setProofType(ProofType proofType) { this.proofType = proofType; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getCorrectionRequestedAt() { return correctionRequestedAt; }
    public void setCorrectionRequestedAt(LocalDateTime correctionRequestedAt) { this.correctionRequestedAt = correctionRequestedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getSellerShippingMethod() { return sellerShippingMethod; }
    public void setSellerShippingMethod(String sellerShippingMethod) { this.sellerShippingMethod = sellerShippingMethod; }

    public String getSellerTrackingNumber() { return sellerTrackingNumber; }
    public void setSellerTrackingNumber(String sellerTrackingNumber) { this.sellerTrackingNumber = sellerTrackingNumber; }

    public String getSellerCourierName() { return sellerCourierName; }
    public void setSellerCourierName(String sellerCourierName) { this.sellerCourierName = sellerCourierName; }

    public LocalDateTime getSellerShipmentDate() { return sellerShipmentDate; }
    public void setSellerShipmentDate(LocalDateTime sellerShipmentDate) { this.sellerShipmentDate = sellerShipmentDate; }

    public String getSellerNote() { return sellerNote; }
    public void setSellerNote(String sellerNote) { this.sellerNote = sellerNote; }

    public String getSellerProofPath() { return sellerProofPath; }
    public void setSellerProofPath(String sellerProofPath) { this.sellerProofPath = sellerProofPath; }

    public Boolean getBuyerReceivedConfirmation() { return buyerReceivedConfirmation; }
    public void setBuyerReceivedConfirmation(Boolean buyerReceivedConfirmation) { this.buyerReceivedConfirmation = buyerReceivedConfirmation; }

    public LocalDateTime getBuyerReceivedDate() { return buyerReceivedDate; }
    public void setBuyerReceivedDate(LocalDateTime buyerReceivedDate) { this.buyerReceivedDate = buyerReceivedDate; }

    public String getBuyerProofPath() { return buyerProofPath; }
    public void setBuyerProofPath(String buyerProofPath) { this.buyerProofPath = buyerProofPath; }

    public String getBuyerNote() { return buyerNote; }
    public void setBuyerNote(String buyerNote) { this.buyerNote = buyerNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<PaymentSubmissionHistoryImage> getImages() { return images; }
    public void setImages(List<PaymentSubmissionHistoryImage> images) { this.images = images; }
}
