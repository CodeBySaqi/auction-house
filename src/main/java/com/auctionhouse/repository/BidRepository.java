package com.auctionhouse.repository;

import com.auctionhouse.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);

    List<Bid> findByBidderIdOrderByTimestampDesc(Long bidderId);

    @Query("SELECT b FROM Bid b WHERE b.bidder.id = :bidderId AND b.auction.id = :auctionId ORDER BY b.amount DESC")
    List<Bid> findByBidderIdAndAuctionId(@Param("bidderId") Long bidderId, @Param("auctionId") Long auctionId);

    @Query("SELECT MAX(b.amount) FROM Bid b WHERE b.auction.id = :auctionId")
    Optional<Double> findHighestBidAmount(@Param("auctionId") Long auctionId);

    long countByAuctionId(Long auctionId);
}
