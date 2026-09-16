package com.auctionhouse.service;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionCategory;
import com.auctionhouse.model.AuctionStatus;
import com.auctionhouse.model.User;
import com.auctionhouse.model.Conversation;
import com.auctionhouse.repository.AuctionRepository;
import com.auctionhouse.repository.BidRepository;
import com.auctionhouse.repository.ChatMessageRepository;
import com.auctionhouse.repository.ConversationRepository;
import com.auctionhouse.repository.PaymentReleaseRepository;
import com.auctionhouse.repository.PlatformCommissionRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AuctionService - handles all auction-related business logic.
 * Demonstrates Abstraction: exposes operations without implementation details.
 */
@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PaymentReleaseService paymentReleaseService;
    private final BidRepository bidRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final PaymentReleaseRepository paymentReleaseRepository;
    private final PlatformCommissionRepository platformCommissionRepository;

    @Autowired
    public AuctionService(AuctionRepository auctionRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         @Lazy PaymentReleaseService paymentReleaseService,
                         BidRepository bidRepository,
                         ChatMessageRepository chatMessageRepository,
                         ConversationRepository conversationRepository,
                         PaymentReleaseRepository paymentReleaseRepository,
                         PlatformCommissionRepository platformCommissionRepository) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.paymentReleaseService = paymentReleaseService;
        this.bidRepository = bidRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationRepository = conversationRepository;
        this.paymentReleaseRepository = paymentReleaseRepository;
        this.platformCommissionRepository = platformCommissionRepository;
    }

    /**
     * Get all active auctions.
     */
    public List<Auction> getActiveAuctions() {
        return auctionRepository.findActiveAuctions(AuctionStatus.ACTIVE, LocalDateTime.now());
    }

    /**
     * Get active auctions by category.
     */
    public List<Auction> getActiveAuctionsByCategory(AuctionCategory category) {
        return auctionRepository.findByCategoryAndStatusOrderByEndTimeAsc(category, AuctionStatus.ACTIVE);
    }

    /**
     * Search auctions by keyword.
     */
    public List<Auction> searchAuctions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActiveAuctions();
        }
        return auctionRepository.searchActiveByKeyword(keyword.trim());
    }

    /**
     * Get featured auctions (top 6 ending soonest).
     */
    public List<Auction> getFeaturedAuctions() {
        return auctionRepository.findTop6ByStatusOrderByEndTimeAsc(AuctionStatus.ACTIVE);
    }

    /**
     * Find auction by ID.
     */
    public Optional<Auction> findById(Long id) {
        return auctionRepository.findById(id);
    }

    /**
     * Save an auction.
     */
    @Transactional
    public Auction save(Auction auction) {
        return auctionRepository.save(auction);
    }

    /**
     * Get count of active auctions.
     */
    public long getActiveCount() {
        return auctionRepository.countByStatus(AuctionStatus.ACTIVE);
    }

    /**
     * Scheduled task: close expired auctions every minute.
     * Determines winners, sends notifications, and creates payment release records.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredAuctions() {
        List<Auction> expired = auctionRepository.findExpiredAuctions(AuctionStatus.ACTIVE, LocalDateTime.now());
        for (Auction auction : expired) {
            User winner = auction.closeAuction();
            auctionRepository.save(auction);

            if (winner != null) {
                notificationService.createNotification(
                        winner,
                        "🏆 Congratulations! You won \"" + auction.getTitle() + "\" for $" +
                                String.format("%,.2f", auction.getCurrentHighestBid()) + "!",
                        "WON",
                        auction.getId()
                );

                // Create payment release record for won auctions
                paymentReleaseService.createPaymentRelease(auction);
            } else {
                notifySellerUnsold(auction);
            }
        }
    }

    /**
     * Get all auctions (for admin/future use).
     */
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    /**
     * Get auctions created by a specific user.
     */
    public List<Auction> getAuctionsByCreator(User user) {
        return auctionRepository.findByCreatedByOrderByCreatedAtDesc(user);
    }

    /**
     * Get all pending auctions awaiting approval.
     */
    public List<Auction> getPendingAuctions() {
        return auctionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.PENDING_APPROVAL);
    }

    /**
     * Get auctions by status.
     */
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        return auctionRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Approve an auction - sets status to ACTIVE and notifies the seller.
     */
    @Transactional
    public void approveAuction(Auction auction, User admin) {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setReviewedBy(admin);
        auction.setReviewedAt(LocalDateTime.now());
        auction.setRejectionReason(null); // Clear any previous rejection reason
        auctionRepository.save(auction);

        // Notify the seller
        if (auction.getCreatedBy() != null) {
            notificationService.createNotification(
                auction.getCreatedBy(),
                "✅ Your auction \"" + auction.getTitle() + "\" has been approved and is now live!",
                "AUCTION_APPROVED",
                auction.getId()
            );
        }
    }

    /**
     * Reject an auction - sets status to REJECTED, saves reason, and notifies the seller.
     */
    @Transactional
    public void rejectAuction(Auction auction, User admin, String reason) {
        auction.setStatus(AuctionStatus.REJECTED);
        auction.setReviewedBy(admin);
        auction.setReviewedAt(LocalDateTime.now());
        auction.setRejectionReason(reason);
        auctionRepository.save(auction);

        // Notify the seller
        if (auction.getCreatedBy() != null) {
            notificationService.createNotification(
                auction.getCreatedBy(),
                "❌ Your auction \"" + auction.getTitle() + "\" has been rejected. Reason: " + reason,
                "AUCTION_REJECTED",
                auction.getId()
            );
        }
    }

    /**
     * Cancel an auction and refund the highest bidder if present.
     * Prevents double-cancellation by checking status first.
     */
    @Transactional
    public void cancelAuction(Auction auction) {
        // Prevent double-cancellation
        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new IllegalStateException("Auction is already cancelled");
        }
        
        refundHighestBidder(auction);
        auction.setStatus(AuctionStatus.CANCELLED);
        auction.setHighestBidder(null);
        auction.setCurrentHighestBid(0);
        auctionRepository.save(auction);
    }

    /**
     * Manually close an auction (admin action). Determines winner same as scheduled job,
     * creates PaymentRelease if there is a winner.
     */
    @Transactional
    public void closeAuctionManually(Auction auction) {
        User winner = auction.closeAuction();
        auctionRepository.save(auction);

        if (winner != null) {
            notificationService.createNotification(
                    winner,
                    "🏆 Congratulations! You won \"" + auction.getTitle() + "\" for $" +
                            String.format("%,.2f", auction.getCurrentHighestBid()) + "!",
                    "WON",
                    auction.getId()
            );
            paymentReleaseService.createPaymentRelease(auction);
        } else {
            notifySellerUnsold(auction);
        }
    }

    /**
     * Delete auction by ID. Refunds the highest bidder first if present.
     * Cleans up all dependent records (bids, conversations, messages, payments, commissions)
     * to avoid foreign key constraint violations.
     */
    @Transactional
    public void deleteById(Long id) {
        Optional<Auction> opt = auctionRepository.findById(id);
        if (opt.isPresent()) {
            Auction auction = opt.get();
            refundHighestBidder(auction);
            
            // Clean up dependent records to avoid FK constraint violations
            // 1. Delete chat messages for the conversation related to this auction
            conversationRepository.findByAuctionId(id).ifPresent(conv -> {
                chatMessageRepository.deleteByConversationId(conv.getId());
            });
            
            // 2. Delete conversations
            conversationRepository.deleteByAuctionId(id);
            
            // 3. Delete platform commissions
            platformCommissionRepository.deleteByAuctionId(id);
            
            // 4. Delete payment releases
            paymentReleaseRepository.deleteByAuctionId(id);
            
            // 5. Delete bids
            bidRepository.deleteByAuctionId(id);
        }
        auctionRepository.deleteById(id);
    }

    /**
     * Refund the current highest bid back to the highest bidder's wallet.
     * Re-fetches the user from the repository to avoid stale data.
     * No-op if there is no highest bidder or bid amount is zero.
     */
    private void refundHighestBidder(Auction auction) {
        User highestBidder = auction.getHighestBidder();
        double bidAmount = auction.getCurrentHighestBid();

        if (highestBidder == null || bidAmount <= 0) {
            return;
        }

        // Re-fetch user to get fresh wallet balance
        User freshBidder = userRepository.findById(highestBidder.getId())
                .orElseThrow(() -> new RuntimeException("Highest bidder not found: " + highestBidder.getId()));

        BigDecimal refund = BigDecimal.valueOf(bidAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentBalance = BigDecimal.valueOf(freshBidder.getWalletBalance()).setScale(2, RoundingMode.HALF_UP);
        freshBidder.setWalletBalance(currentBalance.add(refund).doubleValue());
        userRepository.save(freshBidder);

        // Keep the in-memory reference in sync for callers that use auction.getHighestBidder() after this
        auction.setHighestBidder(freshBidder);

        notificationService.createNotification(
                freshBidder,
                "💸 Auction \"" + auction.getTitle() + "\" was cancelled/closed. $" +
                        String.format("%,.2f", refund.doubleValue()) + " has been refunded to your wallet.",
                "REFUND",
                auction.getId()
        );
    }

    /**
     * Refund the highest bidder and clear bid state without changing auction status.
     * Used for reopen scenarios where we don't want to send a "cancelled" notification.
     */
    @Transactional
    public void clearAuctionBids(Auction auction) {
        refundHighestBidder(auction);
        auction.setHighestBidder(null);
        auction.setCurrentHighestBid(0);
        auction.setBidCount(0);
        auctionRepository.save(auction);
    }

    /**
     * Notify the seller that their auction closed with no winner.
     */
    private void notifySellerUnsold(Auction auction) {
        if (auction.getCreatedBy() != null) {
            notificationService.createNotification(
                    auction.getCreatedBy(),
                    "📦 Your auction \"" + auction.getTitle() + "\" has ended with no winning bids.",
                    "UNSOLD",
                    auction.getId()
            );
        }
    }
}
