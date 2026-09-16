package com.auctionhouse.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PlatformCommission entity - permanent record of the 10% platform commission
 * collected from each completed auction sale.
 *
 * Created once when admin releases payment to seller. Immutable after creation.
 *
 * Example: Winning bid $100 → Commission $10 (10%) → Seller payout $90 (90%)
 */
@Entity
@Table(name = "platform_commissions")
public class PlatformCommission {

    /** Commission percentage applied to all auctions (10%). */
    public static final BigDecimal COMMISSION_RATE = new BigDecimal("10");

    /** Seller payout percentage (90%). */
    public static final BigDecimal SELLER_PAYOUT_RATE = new BigDecimal("90");

    /** Currency scale for all monetary calculations (2 decimal places). */
    public static final int CURRENCY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_release_id", nullable = false, unique = true)
    private PaymentRelease paymentRelease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /** The full winning bid amount (what the buyer paid). */
    @Column(name = "winning_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal winningAmount;

    /** Commission percentage (always 10). */
    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    /** The 10% commission amount retained by the platform. */
    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    /** The 90% payout amount credited to the seller's wallet. */
    @Column(name = "seller_payout_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellerPayoutAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PlatformCommission() {
        this.createdAt = LocalDateTime.now();
    }

    public PlatformCommission(PaymentRelease paymentRelease, Auction auction, User buyer, User seller,
                               BigDecimal winningAmount, BigDecimal commissionPercentage,
                               BigDecimal commissionAmount, BigDecimal sellerPayoutAmount) {
        this.paymentRelease = paymentRelease;
        this.auction = auction;
        this.buyer = buyer;
        this.seller = seller;
        this.winningAmount = winningAmount;
        this.commissionPercentage = commissionPercentage;
        this.commissionAmount = commissionAmount;
        this.sellerPayoutAmount = sellerPayoutAmount;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Calculate commission breakdown for a given winning amount.
     * Uses BigDecimal with HALF_UP rounding to avoid floating-point errors.
     *
     * @param winningAmount the full winning bid
     * @return BigDecimal[3] where [0]=commissionAmount, [1]=sellerPayoutAmount, [2]=commissionPercentage
     */
    public static BigDecimal[] calculate(BigDecimal winningAmount) {
        BigDecimal hundred = new BigDecimal("100");

        // Commission = winningAmount * 10 / 100, rounded to 2 decimal places
        BigDecimal commission = winningAmount
                .multiply(COMMISSION_RATE)
                .divide(hundred, CURRENCY_SCALE, java.math.RoundingMode.HALF_UP);

        // Seller payout = winningAmount - commission (avoids rounding drift)
        BigDecimal sellerPayout = winningAmount.subtract(commission);

        return new BigDecimal[]{commission, sellerPayout, COMMISSION_RATE};
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PaymentRelease getPaymentRelease() { return paymentRelease; }
    public void setPaymentRelease(PaymentRelease paymentRelease) { this.paymentRelease = paymentRelease; }

    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }

    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public BigDecimal getWinningAmount() { return winningAmount; }
    public void setWinningAmount(BigDecimal winningAmount) { this.winningAmount = winningAmount; }

    public BigDecimal getCommissionPercentage() { return commissionPercentage; }
    public void setCommissionPercentage(BigDecimal commissionPercentage) { this.commissionPercentage = commissionPercentage; }

    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }

    public BigDecimal getSellerPayoutAmount() { return sellerPayoutAmount; }
    public void setSellerPayoutAmount(BigDecimal sellerPayoutAmount) { this.sellerPayoutAmount = sellerPayoutAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
