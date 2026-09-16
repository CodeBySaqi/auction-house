package com.auctionhouse.repository;

import com.auctionhouse.model.Conversation;
import com.auctionhouse.model.PaymentRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Conversation entities.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    Optional<Conversation> findByPaymentRelease(PaymentRelease paymentRelease);
    
    Optional<Conversation> findByAuctionId(Long auctionId);
    
    boolean existsByPaymentRelease(PaymentRelease paymentRelease);
    
    @Query("SELECT c FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findByBuyerIdOrSellerId(@Param("userId") Long userId);
}
