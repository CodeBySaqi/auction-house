package com.auctionhouse.repository;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.PlatformCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformCommissionRepository extends JpaRepository<PlatformCommission, Long> {

    Optional<PlatformCommission> findByPaymentRelease(PaymentRelease paymentRelease);

    Optional<PlatformCommission> findByAuction(Auction auction);

    boolean existsByPaymentRelease(PaymentRelease paymentRelease);

    boolean existsByAuction(Auction auction);

    void deleteByAuctionId(Long auctionId);

    @Query("SELECT COALESCE(SUM(pc.commissionAmount), 0) FROM PlatformCommission pc")
    BigDecimal sumTotalCommission();

    @Query("SELECT COALESCE(SUM(pc.sellerPayoutAmount), 0) FROM PlatformCommission pc")
    BigDecimal sumTotalSellerPayout();

    @Query("SELECT COALESCE(SUM(pc.winningAmount), 0) FROM PlatformCommission pc")
    BigDecimal sumTotalWinningAmount();

    @Query("SELECT pc FROM PlatformCommission pc ORDER BY pc.createdAt DESC")
    List<PlatformCommission> findAllOrderByCreatedAtDesc();
}
