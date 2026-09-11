package com.auctionhouse.service;

import com.auctionhouse.exception.AuctionClosedException;
import com.auctionhouse.exception.BidTooLowException;
import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionStatus;
import com.auctionhouse.model.Bid;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.BidRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final NotificationService notificationService;

    @Autowired
    public BidService(BidRepository bidRepository, UserRepository userRepository, NotificationService notificationService) {
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Place a bid on an auction. Full validation, wallet deduction, and notifications.
     */
    @Transactional
    public Bid placeBid(Auction auction, User bidder, double amount) {
        // Validation 1: Auction must be active
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionClosedException("This auction is no longer active.");
        }

        // Validation 2: Auction must not be expired
        if (auction.isExpired()) {
            throw new AuctionClosedException("This auction has ended.");
        }

        // Validation 3: Bid must meet minimum requirement
        double minimumBid = auction.getMinimumBid();
        if (amount < minimumBid) {
            throw new BidTooLowException(
                    String.format("Bid must be at least $%,.2f (current: $%,.2f)",
                            minimumBid, auction.getCurrentHighestBid()));
        }

        // Validation 4: Bid must be above starting price
        if (amount < auction.getStartingPrice()) {
            throw new BidTooLowException(
                    String.format("Bid must be at least the starting price of $%,.2f",
                            auction.getStartingPrice()));
        }

        // Validation 5: User must have enough wallet balance
        if (bidder.getWalletBalance() < amount) {
            throw new BidTooLowException(
                    String.format("Insufficient balance. You have $%,.2f but need $%,.2f. Get more money from your dashboard!",
                            bidder.getWalletBalance(), amount));
        }

        // Store previous highest bidder for refund + notification
        User previousHighest = auction.getHighestBidder();
        double previousBidAmount = auction.getCurrentHighestBid();

        // REFUND the previous highest bidder (their money is unlocked)
        if (previousHighest != null && !previousHighest.getId().equals(bidder.getId())) {
            previousHighest.setWalletBalance(previousHighest.getWalletBalance() + previousBidAmount);
            userRepository.save(previousHighest);

            notificationService.createNotification(
                    previousHighest,
                    "⚠️ You've been outbid on \"" + auction.getTitle() +
                            "\"! $"+ String.format("%,.2f", previousBidAmount) + " refunded to your wallet. New highest bid: $" + String.format("%,.2f", amount),
                    "OUTBID",
                    auction.getId()
            );
        }

        // DEDUCT bid amount from current bidder's wallet
        bidder.setWalletBalance(bidder.getWalletBalance() - amount);
        userRepository.save(bidder);

        // Create the bid
        Bid bid = new Bid(bidder, auction, amount);

        // Accept the bid (updates auction state - polymorphic)
        boolean accepted = auction.acceptBid(bid);
        if (!accepted) {
            // Refund if something went wrong
            bidder.setWalletBalance(bidder.getWalletBalance() + amount);
            userRepository.save(bidder);
            throw new BidTooLowException("Bid could not be accepted. Please try a higher amount.");
        }

        // Save the bid
        Bid savedBid = bidRepository.save(bid);

        // Notify the bidder their bid was placed
        notificationService.createNotification(
                bidder,
                "✅ Your bid of $" + String.format("%,.2f", amount) +
                        " on \"" + auction.getTitle() + "\" is now the highest! $" +
                        String.format("%,.2f", amount) + " locked from your wallet.",
                "BID_PLACED",
                auction.getId()
        );

        return savedBid;
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
