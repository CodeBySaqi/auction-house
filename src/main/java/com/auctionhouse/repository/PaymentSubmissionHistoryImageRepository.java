package com.auctionhouse.repository;

import com.auctionhouse.model.PaymentSubmissionHistoryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSubmissionHistoryImageRepository extends JpaRepository<PaymentSubmissionHistoryImage, Long> {
}
