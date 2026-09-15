package com.auctionhouse.repository;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionCategory;
import com.auctionhouse.model.AuctionStatus;
import com.auctionhouse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatusOrderByEndTimeAsc(AuctionStatus status);

    List<Auction> findByCategoryAndStatusOrderByEndTimeAsc(AuctionCategory category, AuctionStatus status);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endTime > :now ORDER BY a.endTime ASC")
    List<Auction> findActiveAuctions(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endTime <= :now")
    List<Auction> findExpiredAuctions(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auction a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Auction> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY a.endTime ASC")
    List<Auction> searchActiveByKeyword(@Param("keyword") String keyword);

    long countByStatus(AuctionStatus status);

    List<Auction> findTop6ByStatusOrderByEndTimeAsc(AuctionStatus status);

    List<Auction> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    /* ---------- Admin console additions ---------- */

    /** Auctions whose end time falls inside a window (used for "ending soon"). */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endTime BETWEEN :from AND :to ORDER BY a.endTime ASC")
    List<Auction> findEndingBetween(@Param("status") AuctionStatus status,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    /** Count of items created since a point in time. */
    long countByCreatedAtAfter(LocalDateTime since);

    /** Everything, newest first, for the moderation table. */
    List<Auction> findAllByOrderByCreatedAtDesc();

    /** Count per category, without loading every row. */
    @Query("SELECT a.category, COUNT(a) FROM Auction a GROUP BY a.category")
    List<Object[]> countGroupedByCategory();

    /** Count per status. */
    @Query("SELECT a.status, COUNT(a) FROM Auction a GROUP BY a.status")
    List<Object[]> countGroupedByStatus();
}
