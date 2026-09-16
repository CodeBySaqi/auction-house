package com.auctionhouse.service;

import com.auctionhouse.model.*;
import com.auctionhouse.repository.AuctionRepository;
import com.auctionhouse.repository.PaymentReleaseRepository;
import com.auctionhouse.repository.PlatformCommissionRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * PaymentReleaseService - handles buyer/seller verification and admin payment release.
 */
@Service
public class PaymentReleaseService {

    private final PaymentReleaseRepository paymentReleaseRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final PlatformCommissionRepository platformCommissionRepository;

    @Autowired
    public PaymentReleaseService(PaymentReleaseRepository paymentReleaseRepository,
                                  AuctionRepository auctionRepository,
                                  UserRepository userRepository,
                                  NotificationService notificationService,
                                  FileStorageService fileStorageService,
                                  PlatformCommissionRepository platformCommissionRepository) {
        this.paymentReleaseRepository = paymentReleaseRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
        this.platformCommissionRepository = platformCommissionRepository;
    }

    /**
     * Create a payment release record when an auction is won.
     * Called from AuctionService.closeExpiredAuctions()
     */
    @Transactional
    public PaymentRelease createPaymentRelease(Auction auction) {
        if (paymentReleaseRepository.existsByAuction(auction)) {
            return paymentReleaseRepository.findByAuction(auction).orElse(null);
        }

        User buyer = auction.getHighestBidder();
        User seller = auction.getCreatedBy();
        
        if (buyer == null || seller == null) {
            return null; // No winner or seller
        }

        BigDecimal winningAmount = BigDecimal.valueOf(auction.getCurrentHighestBid());
        PaymentRelease paymentRelease = new PaymentRelease(auction, buyer, seller, winningAmount);
        
        PaymentRelease saved = paymentReleaseRepository.save(paymentRelease);

        // Notify both parties
        notificationService.createNotification(
            seller,
            "🎉 Your auction \"" + auction.getTitle() + "\" has been won! Please submit delivery details. You will receive 90% of the winning bid (after 10% platform commission).",
            "AUCTION_WON_SELLER",
            auction.getId()
        );

        notificationService.createNotification(
            buyer,
            "📦 You won \"" + auction.getTitle() + "\"! Please submit received-item details after delivery.",
            "AUCTION_WON_BUYER",
            auction.getId()
        );

        return saved;
    }

    /**
     * Seller submits delivery/shipping details.
     */
    @Transactional
    public void submitSellerDetails(Long paymentReleaseId, User seller, 
                                     String shippingMethod, String trackingNumber,
                                     String courierName, LocalDateTime shipmentDate,
                                     String proofPath, String note) {
        PaymentRelease pr = paymentReleaseRepository.findById(paymentReleaseId)
            .orElseThrow(() -> new RuntimeException("Payment release not found"));

        // Security: Only the seller can submit seller details
        if (!pr.getSeller().getId().equals(seller.getId())) {
            throw new SecurityException("You are not authorized to submit details for this auction.");
        }

        // Check if already submitted
        if (pr.isSellerDetailsSubmitted()) {
            throw new IllegalStateException("Seller details already submitted.");
        }

        // Update seller details
        pr.setSellerShippingMethod(shippingMethod);
        pr.setSellerTrackingNumber(trackingNumber);
        pr.setSellerCourierName(courierName);
        pr.setSellerShipmentDate(shipmentDate);
        pr.setSellerProofPath(proofPath);
        pr.setSellerNote(note);
        pr.setSellerSubmittedAt(LocalDateTime.now());

        // Update status
        updateStatus(pr);

        paymentReleaseRepository.save(pr);

        // Notify buyer
        notificationService.createNotification(
            pr.getBuyer(),
            "📮 Seller has submitted delivery details for \"" + pr.getAuction().getTitle() + "\". Tracking: " + 
            (trackingNumber != null ? trackingNumber : "N/A"),
            "SELLER_DETAILS_SUBMITTED",
            pr.getAuction().getId()
        );
    }

    /**
     * Buyer submits received-item details.
     */
    @Transactional
    public void submitBuyerDetails(Long paymentReleaseId, User buyer,
                                    Boolean receivedConfirmation, LocalDateTime receivedDate,
                                    String proofPath, String note) {
        PaymentRelease pr = paymentReleaseRepository.findById(paymentReleaseId)
            .orElseThrow(() -> new RuntimeException("Payment release not found"));

        // Security: Only the buyer can submit buyer details
        if (!pr.getBuyer().getId().equals(buyer.getId())) {
            throw new SecurityException("You are not authorized to submit details for this auction.");
        }

        // Check if already submitted
        if (pr.isBuyerDetailsSubmitted()) {
            throw new IllegalStateException("Buyer details already submitted.");
        }

        // Update buyer details
        pr.setBuyerReceivedConfirmation(receivedConfirmation);
        pr.setBuyerReceivedDate(receivedDate);
        pr.setBuyerProofPath(proofPath);
        pr.setBuyerNote(note);
        pr.setBuyerSubmittedAt(LocalDateTime.now());

        // Update status
        updateStatus(pr);

        paymentReleaseRepository.save(pr);

        // Notify seller
        notificationService.createNotification(
            pr.getSeller(),
            "✅ Buyer has submitted received-item details for \"" + pr.getAuction().getTitle() + "\". Awaiting admin review.",
            "BUYER_DETAILS_SUBMITTED",
            pr.getAuction().getId()
        );
    }

    /**
     * Admin verifies the case and approves for payment release.
     */
    @Transactional
    public void verifyPaymentRelease(Long paymentReleaseId, User admin) {
        PaymentRelease pr = paymentReleaseRepository.findById(paymentReleaseId)
            .orElseThrow(() -> new RuntimeException("Payment release not found"));

        // Security: Only admin can verify
        if (!isAdmin(admin)) {
            throw new SecurityException("Only administrators can verify payment releases.");
        }

        // Must be ready for review
        if (!pr.isReadyForReview()) {
            throw new IllegalStateException("Payment release is not ready for review. Both parties must submit details first.");
        }

        pr.setReviewedBy(admin);
        pr.setReviewedAt(LocalDateTime.now());
        pr.setStatus(VerificationStatus.VERIFIED);
        pr.setRejectionReason(null); // Clear any previous rejection

        paymentReleaseRepository.save(pr);

        // Notify both parties
        notificationService.createNotification(
            pr.getSeller(),
            "✅ Your payment for \"" + pr.getAuction().getTitle() + "\" has been verified and will be released shortly.",
            "PAYMENT_VERIFIED",
            pr.getAuction().getId()
        );

        notificationService.createNotification(
            pr.getBuyer(),
            "✅ Delivery verification complete for \"" + pr.getAuction().getTitle() + "\".",
            "PAYMENT_VERIFIED",
            pr.getAuction().getId()
        );
    }

    /**
     * Admin rejects the case and requests correction.
     */
    @Transactional
    public void rejectPaymentRelease(Long paymentReleaseId, User admin, String reason) {
        PaymentRelease pr = paymentReleaseRepository.findById(paymentReleaseId)
            .orElseThrow(() -> new RuntimeException("Payment release not found"));

        // Security: Only admin can reject
        if (!isAdmin(admin)) {
            throw new SecurityException("Only administrators can reject payment releases.");
        }

        pr.setReviewedBy(admin);
        pr.setReviewedAt(LocalDateTime.now());
        pr.setStatus(VerificationStatus.NEEDS_CORRECTION);
        pr.setRejectionReason(reason);

        // Clear submissions so they can resubmit
        pr.setSellerSubmittedAt(null);
        pr.setBuyerSubmittedAt(null);

        paymentReleaseRepository.save(pr);

        // Notify both parties
        String message = "❌ Verification for \"" + pr.getAuction().getTitle() + "\" needs correction. Reason: " + reason;
        
        notificationService.createNotification(pr.getSeller(), message, "PAYMENT_REJECTED", pr.getAuction().getId());
        notificationService.createNotification(pr.getBuyer(), message, "PAYMENT_REJECTED", pr.getAuction().getId());
    }

    /**
     * Admin releases payment to seller with 10% platform commission.
     * IDEMPOTENT - can only release once.
     * 
     * Commission calculation (server-side, BigDecimal):
     * - Commission = winningAmount × 10% (rounded HALF_UP to 2 decimals)
     * - Seller payout = winningAmount - commission
     * 
     * Example: $100 winning → $10 commission → $90 seller payout
     */
    @Transactional
    public void releasePayment(Long paymentReleaseId, User admin) {
        PaymentRelease pr = paymentReleaseRepository.findById(paymentReleaseId)
            .orElseThrow(() -> new RuntimeException("Payment release not found"));

        // Security: Only admin can release
        if (!isAdmin(admin)) {
            throw new SecurityException("Only administrators can release payments.");
        }

        // IDEMPOTENT: Check if already released
        if (pr.isPaymentReleased()) {
            throw new IllegalStateException("Payment has already been released for this auction.");
        }

        // Must be verified first
        if (pr.getStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Payment release must be verified before releasing payment.");
        }

        // IDEMPOTENT: Check if commission already exists (belt-and-suspenders)
        if (platformCommissionRepository.existsByPaymentRelease(pr)) {
            throw new IllegalStateException("Commission already recorded for this auction.");
        }

        // ---- COMMISSION CALCULATION (BigDecimal, server-side) ----
        BigDecimal winningAmount = pr.getWinningAmount();
        BigDecimal[] breakdown = PlatformCommission.calculate(winningAmount);
        BigDecimal commissionAmount = breakdown[0];   // 10%
        BigDecimal sellerPayoutAmount = breakdown[1]; // 90%

        // ---- CREDIT SELLER'S WALLET (90% only, NOT full amount) ----
        User seller = userRepository.findById(pr.getSeller().getId())
            .orElseThrow(() -> new RuntimeException("Seller not found"));

        BigDecimal currentBalance = BigDecimal.valueOf(seller.getWalletBalance());
        BigDecimal newBalance = currentBalance.add(sellerPayoutAmount);
        seller.setWalletBalance(newBalance.doubleValue());
        userRepository.save(seller);

        // ---- UPDATE PAYMENT RELEASE RECORD ----
        pr.setReleasedBy(admin);
        pr.setReleasedAt(LocalDateTime.now());
        pr.setPaymentReleased(true);
        pr.setStatus(VerificationStatus.PAYMENT_RELEASED);
        pr.setCommissionAmount(commissionAmount);
        pr.setSellerPayoutAmount(sellerPayoutAmount);
        paymentReleaseRepository.save(pr);

        // ---- CREATE PERMANENT COMMISSION RECORD (idempotent) ----
        PlatformCommission commission = new PlatformCommission(
            pr,
            pr.getAuction(),
            pr.getBuyer(),
            seller,
            winningAmount,
            PlatformCommission.COMMISSION_RATE,
            commissionAmount,
            sellerPayoutAmount
        );
        platformCommissionRepository.save(commission);

        // ---- NOTIFICATIONS ----
        notificationService.createNotification(
            seller,
            "💰 Payment of $" + String.format("%,.2f", sellerPayoutAmount.doubleValue()) +
            " has been released to your wallet for \"" + pr.getAuction().getTitle() +
            "\" (after 10% platform commission of $" + String.format("%,.2f", commissionAmount.doubleValue()) + ").",
            "PAYMENT_RELEASED",
            pr.getAuction().getId()
        );

        notificationService.createNotification(
            pr.getBuyer(),
            "✅ Transaction complete for \"" + pr.getAuction().getTitle() +
            "\". Seller has been paid.",
            "PAYMENT_RELEASED",
            pr.getAuction().getId()
        );
    }

    /**
     * Get all payment releases.
     */
    public List<PaymentRelease> getAllPaymentReleases() {
        return paymentReleaseRepository.findAll();
    }

    /**
     * Find payment release by ID.
     */
    public Optional<PaymentRelease> findById(Long id) {
        return paymentReleaseRepository.findById(id);
    }

    /**
     * Get payment release by auction ID.
     */
    public Optional<PaymentRelease> findByAuctionId(Long auctionId) {
        return paymentReleaseRepository.findByAuctionId(auctionId);
    }

    /**
     * Get all payment releases for a seller.
     */
    public List<PaymentRelease> getBySeller(User seller) {
        return paymentReleaseRepository.findBySeller(seller);
    }

    /**
     * Get all payment releases for a buyer.
     */
    public List<PaymentRelease> getByBuyer(User buyer) {
        return paymentReleaseRepository.findByBuyer(buyer);
    }

    /**
     * Get all payment releases pending admin review.
     */
    public List<PaymentRelease> getPendingAdminReview() {
        return paymentReleaseRepository.findPendingAdminReview();
    }

    /**
     * Get count of payment releases pending review.
     */
    public long countPendingReview() {
        return paymentReleaseRepository.countPendingReview();
    }

    /**
     * Get all released payments.
     */
    public List<PaymentRelease> getReleasedPayments() {
        return paymentReleaseRepository.findReleasedPayments();
    }

    /**
     * Update status based on submission state.
     */
    private void updateStatus(PaymentRelease pr) {
        boolean sellerSubmitted = pr.isSellerDetailsSubmitted();
        boolean buyerSubmitted = pr.isBuyerDetailsSubmitted();

        if (sellerSubmitted && buyerSubmitted) {
            pr.setStatus(VerificationStatus.READY_FOR_ADMIN_REVIEW);
        } else if (sellerSubmitted) {
            pr.setStatus(VerificationStatus.SELLER_DETAILS_SUBMITTED);
        } else if (buyerSubmitted) {
            pr.setStatus(VerificationStatus.BUYER_DETAILS_SUBMITTED);
        } else {
            pr.setStatus(VerificationStatus.WAITING_FOR_DETAILS);
        }
    }

    /**
     * Check if user is admin.
     */
    private boolean isAdmin(User user) {
        return user.getRole().equals("ROLE_ADMIN") || user.getRole().equals("ROLE_SUPER_ADMIN");
    }

    // ---- COMMISSION QUERY METHODS ----

    /**
     * Get all platform commissions, newest first.
     */
    public List<PlatformCommission> getAllCommissions() {
        return platformCommissionRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * Get total commission collected across all auctions.
     */
    public BigDecimal getTotalCommission() {
        return platformCommissionRepository.sumTotalCommission();
    }

    /**
     * Get total seller payouts across all auctions.
     */
    public BigDecimal getTotalSellerPayout() {
        return platformCommissionRepository.sumTotalSellerPayout();
    }

    /**
     * Get total winning amounts across all commissioned auctions.
     */
    public BigDecimal getTotalWinningAmount() {
        return platformCommissionRepository.sumTotalWinningAmount();
    }

    /**
     * Get commission record for a specific payment release.
     */
    public Optional<PlatformCommission> getCommissionByPaymentRelease(PaymentRelease pr) {
        return platformCommissionRepository.findByPaymentRelease(pr);
    }

    /**
     * Get total number of commission records.
     */
    public long getCommissionCount() {
        return platformCommissionRepository.count();
    }
}
