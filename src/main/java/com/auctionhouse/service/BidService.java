package com.auctionhouse.service;

import com.auctionhouse.exception.AuctionClosedException;
import com.auctionhouse.exception.BidTooLowException;
import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionStatus;
import com.auctionhouse.model.Bid;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.AuctionRepository;
import com.auctionhouse.repository.BidRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * BidService - handles all bidding logic.
 * Core business rules for placing and validating bids.
 * Wallet balance is deducted on bid and refunded when outbid.
 */
@Service
public class BidService {

    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final NotificationService notificationService;

    @Autowired
    public BidService(BidRepository bidRepository, UserRepository userRepository, 
                      AuctionRepository auctionRepository, NotificationService notificationService) {
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.notificationService = notificationService;
    }

    /**
     * Place a bid on an auction. Full validation, wallet deduction, and notifications.
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional
    public Bid placeBid(Long auctionId, User bidder, double amount) {
        // Round bid amount to 2 decimal places to prevent floating point issues
        amount = roundToTwoDecimals(amount);

        // Validation 0: Bid amount must be positive
        if (amount <= 0) {
            throw new BidTooLowException("Bid amount must be greater than zero.");
        }

        // Fetch auction with pessimistic lock to prevent concurrent bid corruption
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // Validation 1: Auction must be active (not pending, rejected, closed, or cancelled)
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            if (auction.getStatus() == AuctionStatus.PENDING_APPROVAL) {
                throw new AuctionClosedException("This auction is pending approval and not yet available for bidding.");
            } else if (auction.getStatus() == AuctionStatus.REJECTED) {
                throw new AuctionClosedException("This auction has been rejected and is not available for bidding.");
            } else {
                throw new AuctionClosedException("This auction is no longer active.");
            }
        }

        // Validation 2: Auction must not be expired
        if (auction.isExpired()) {
            throw new AuctionClosedException("This auction has ended.");
        }

        // Validation 3: Seller cannot bid on their own auction
        if (auction.getCreatedBy() != null && auction.getCreatedBy().getId().equals(bidder.getId())) {
            throw new BidTooLowException("You cannot bid on your own auction.");
        }

        // Validation 4: Bid must meet minimum requirement
        double minimumBid = auction.getMinimumBid();
        if (amount < minimumBid) {
            throw new BidTooLowException(
                    String.format("Bid must be at least $%,.2f (current: $%,.2f)",
                            minimumBid, auction.getCurrentHighestBid()));
        }

        // Validation 5: Bid must be above starting price
        if (amount < auction.getStartingPrice()) {
            throw new BidTooLowException(
                    String.format("Bid must be at least the starting price of $%,.2f",
                            auction.getStartingPrice()));
        }

        // Validation 6: User cannot place consecutive bids on the same auction
        // This prevents self-bidding wars and protects bidders from over-committing
        if (auction.getHighestBidder() != null && auction.getHighestBidder().getId().equals(bidder.getId())) {
            throw new BidTooLowException("You are already the highest bidder on this auction. Wait for another bidder before placing a new bid.");
        }

        // Store previous highest bidder for refund + notification
        User previousHighest = auction.getHighestBidder();
        double previousBidAmount = auction.getCurrentHighestBid();

        // REFUND the previous highest bidder (their money is unlocked)
        // Since consecutive bids are blocked, previousHighest is always a different user (or null)
        if (previousHighest != null) {
            double refundAmount = roundToTwoDecimals(previousBidAmount);
            previousHighest.setWalletBalance(roundToTwoDecimals(previousHighest.getWalletBalance() + refundAmount));
            userRepository.save(previousHighest);

            notificationService.createNotification(
                    previousHighest,
                    "⚠️ You've been outbid on \"" + auction.getTitle() +
                            "\"! $"+ String.format("%,.2f", refundAmount) + " refunded to your wallet. New highest bid: $" + String.format("%,.2f", amount),
                    "OUTBID",
                    auction.getId()
            );
        }

        // DEDUCT full bid amount from current bidder's wallet
        double amountToDeduct = amount;

        // Refresh bidder from database to get latest wallet balance
        bidder = userRepository.findById(bidder.getId())
                .orElseThrow(() -> new RuntimeException("Bidder not found"));

        // Validation: User must have enough balance for the deduction
        if (bidder.getWalletBalance() < amountToDeduct) {
            throw new BidTooLowException(
                    String.format("Insufficient balance. You need $%,.2f but have $%,.2f. Get more money from your dashboard!",
                            amountToDeduct, bidder.getWalletBalance()));
        }

        bidder.setWalletBalance(roundToTwoDecimals(bidder.getWalletBalance() - amountToDeduct));
        userRepository.save(bidder);

        // Create the bid
        Bid bid = new Bid(bidder, auction, amount);

        // Accept the bid (updates auction state - polymorphic)
        boolean accepted = auction.acceptBid(bid);
        if (!accepted) {
            // Refund if something went wrong
            bidder.setWalletBalance(roundToTwoDecimals(bidder.getWalletBalance() + amountToDeduct));
            userRepository.save(bidder);
            throw new BidTooLowException("Bid could not be accepted. Please try a higher amount.");
        }

        // Save the auction (with updated highest bid)
        auctionRepository.save(auction);

        // Save the bid
        Bid savedBid = bidRepository.save(bid);

        // Notify the bidder their bid was placed
        String bidMessage = "✅ Your bid of $" + String.format("%,.2f", amount) +
                  " on \"" + auction.getTitle() + "\" is now the highest! $" +
                  String.format("%,.2f", amount) + " locked from your wallet.";

        notificationService.createNotification(
                bidder,
                bidMessage,
                "BID_PLACED",
                auction.getId()
        );

        return savedBid;
    }

    /**
     * Round a double value to 2 decimal places to prevent floating point precision issues.
     */
    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Get all bids for a specific auction, sorted by amount descending.
     */
    public List<Bid> getBidsByAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId);
    }

    /**
     * Get all bids placed by a user.
     */
    public List<Bid> getBidsByUser(Long userId) {
        return bidRepository.findByBidderIdOrderByTimestampDesc(userId);
    }

    /**
     * Get total bid count for an auction.
     */
    public long getBidCount(Long auctionId) {
        return bidRepository.countByAuctionId(auctionId);
    }
}
