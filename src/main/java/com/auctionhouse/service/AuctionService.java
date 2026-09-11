package com.auctionhouse.service;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionCategory;
import com.auctionhouse.model.AuctionStatus;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final NotificationService notificationService;

    @Autowired
    public AuctionService(AuctionRepository auctionRepository, NotificationService notificationService) {
        this.auctionRepository = auctionRepository;
        this.notificationService = notificationService;
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
     * Determines winners and sends notifications.
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
}
