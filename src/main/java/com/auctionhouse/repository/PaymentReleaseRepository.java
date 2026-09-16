package com.auctionhouse.repository;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.User;
import com.auctionhouse.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentReleaseRepository extends JpaRepository<PaymentRelease, Long> {

    Optional<PaymentRelease> findByAuction(Auction auction);

    Optional<PaymentRelease> findByAuctionId(Long auctionId);

    List<PaymentRelease> findBySeller(User seller);

    List<PaymentRelease> findByBuyer(User buyer);

    List<PaymentRelease> findByStatus(VerificationStatus status);

    @Query("SELECT pr FROM PaymentRelease pr WHERE pr.status IN :statuses ORDER BY pr.createdAt DESC")
    List<PaymentRelease> findByStatusIn(@Param("statuses") List<VerificationStatus> statuses);

    @Query("SELECT pr FROM PaymentRelease pr WHERE pr.status = 'READY_FOR_ADMIN_REVIEW' OR pr.status = 'VERIFIED' ORDER BY pr.createdAt DESC")
    List<PaymentRelease> findPendingAdminReview();

    @Query("SELECT pr FROM PaymentRelease pr WHERE pr.paymentReleased = true ORDER BY pr.releasedAt DESC")
    List<PaymentRelease> findReleasedPayments();

    boolean existsByAuction(Auction auction);

    void deleteByAuctionId(Long auctionId);

    @Query("SELECT COUNT(pr) FROM PaymentRelease pr WHERE pr.status = 'READY_FOR_ADMIN_REVIEW'")
    long countPendingReview();
}
