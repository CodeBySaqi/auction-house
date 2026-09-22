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

    void deleteByAuctionId(Long auctionId);

    boolean existsByPaymentRelease(PaymentRelease paymentRelease);
    
    @Query("SELECT c FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByBuyerIdOrSellerId(@Param("userId") Long userId);

    /**
     * Find all conversations (DM + auction) for a user, ordered by last message time.
     */
    @Query("SELECT c FROM Conversation c WHERE (c.buyer.id = :userId OR c.seller.id = :userId) ORDER BY c.lastMessageAt DESC")
    List<Conversation> findAllByUserId(@Param("userId") Long userId);

    /**
     * Find an existing DM conversation between two specific users.
     */
    @Query("SELECT c FROM Conversation c WHERE c.directMessage = true AND " +
           "((c.buyer.id = :user1Id AND c.seller.id = :user2Id) OR " +
           "(c.buyer.id = :user2Id AND c.seller.id = :user1Id))")
    Optional<Conversation> findDirectConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
