package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * PaymentProofImage - one photo of a payment proof.
 * Allows seller and buyer to upload multiple photos as proof.
 * Linked to PaymentRelease with a type discriminator (SELLER / BUYER).
 */
@Entity
@Table(name = "payment_proof_images")
public class PaymentProofImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_release_id", nullable = false)
    private PaymentRelease paymentRelease;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 20)
    private ProofType proofType;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PaymentProofImage() {
        this.createdAt = LocalDateTime.now();
    }

    public PaymentProofImage(PaymentRelease paymentRelease, ProofType proofType, String url, int sortOrder) {
        this.paymentRelease = paymentRelease;
        this.proofType = proofType;
        this.url = url;
        this.sortOrder = sortOrder;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PaymentRelease getPaymentRelease() { return paymentRelease; }
    public void setPaymentRelease(PaymentRelease paymentRelease) { this.paymentRelease = paymentRelease; }

    public ProofType getProofType() { return proofType; }
    public void setProofType(ProofType proofType) { this.proofType = proofType; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
