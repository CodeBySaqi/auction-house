package com.auctionhouse.repository;

import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.PaymentSubmissionHistory;
import com.auctionhouse.model.ProofType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentSubmissionHistoryRepository extends JpaRepository<PaymentSubmissionHistory, Long> {

    List<PaymentSubmissionHistory> findByPaymentReleaseOrderByCreatedAtDesc(PaymentRelease paymentRelease);

    List<PaymentSubmissionHistory> findByPaymentReleaseAndProofTypeOrderByCreatedAtDesc(PaymentRelease paymentRelease, ProofType proofType);

    @Modifying
    @Query("DELETE FROM PaymentSubmissionHistory h WHERE h.paymentRelease.id IN (SELECT pr.id FROM PaymentRelease pr WHERE pr.auction.id = :auctionId)")
    void deleteByAuctionId(@Param("auctionId") Long auctionId);
}
