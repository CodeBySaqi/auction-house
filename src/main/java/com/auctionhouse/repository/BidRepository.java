package com.auctionhouse.repository;

import com.auctionhouse.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuctionIdOrderByTimestampDesc(Long auctionId);

    List<Bid> findByBidderIdOrderByTimestampDesc(Long bidderId);

    @Query("SELECT b FROM Bid b WHERE b.bidder.id = :bidderId AND b.auction.id = :auctionId ORDER BY b.amount DESC")
    List<Bid> findByBidderIdAndAuctionId(@Param("bidderId") Long bidderId, @Param("auctionId") Long auctionId);

    @Query("SELECT MAX(b.amount) FROM Bid b WHERE b.auction.id = :auctionId")
    Optional<Double> findHighestBidAmount(@Param("auctionId") Long auctionId);

    long countByAuctionId(Long auctionId);

    void deleteByAuctionId(Long auctionId);

    long countByBidderId(Long bidderId);

    /**
     * Delete duplicate bids — keeps only the first (lowest ID) for each
     * (auction_id, bidder_id, amount, timestamp) combination.
     */
    @Modifying
    @Query(value = "DELETE FROM bids WHERE id NOT IN " +
                   "(SELECT MIN(id) FROM bids GROUP BY auction_id, bidder_id, amount, timestamp)",
           nativeQuery = true)
    int deleteDuplicateBids();

    /* ---------- Admin console additions ---------- */

    /** Newest bids first, for the admin activity feed. */
    @Query("SELECT b FROM Bid b ORDER BY b.timestamp DESC")
    List<Bid> findRecentBids(org.springframework.data.domain.Pageable pageable);

    /** Bid volume since a point in time. */
    long countByTimestampAfter(LocalDateTime since);

    /** Every bid inside a window, oldest first, for day-by-day aggregation. */
    @Query("SELECT b FROM Bid b WHERE b.timestamp >= :since ORDER BY b.timestamp ASC")
    List<Bid> findBidsSince(@Param("since") LocalDateTime since);

    /** Bid totals per bidder in one query, to avoid N+1 on the accounts table. */
    @Query("SELECT b.bidder.id, COUNT(b) FROM Bid b GROUP BY b.bidder.id")
    List<Object[]> countBidsGroupedByBidder();
}
