package com.auctionhouse.repository;

import com.auctionhouse.model.PaymentProofImage;
import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.ProofType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentProofImageRepository extends JpaRepository<PaymentProofImage, Long> {

    List<PaymentProofImage> findByPaymentReleaseOrderBySortOrderAsc(PaymentRelease paymentRelease);

    List<PaymentProofImage> findByPaymentReleaseAndProofTypeOrderBySortOrderAsc(PaymentRelease paymentRelease, ProofType proofType);

    void deleteByPaymentReleaseAndProofType(PaymentRelease paymentRelease, ProofType proofType);

    void deleteByPaymentRelease(PaymentRelease paymentRelease);

    @Modifying
    @Query("DELETE FROM PaymentProofImage ppi WHERE ppi.paymentRelease.id IN (SELECT pr.id FROM PaymentRelease pr WHERE pr.auction.id = :auctionId)")
    void deleteByAuctionId(@Param("auctionId") Long auctionId);
}
