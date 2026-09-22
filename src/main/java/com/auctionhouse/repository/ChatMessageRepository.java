package com.auctionhouse.repository;

import com.auctionhouse.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ChatMessage entities.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC")
    List<ChatMessage> findByConversationId(@Param("conversationId") Long conversationId);

    void deleteByConversationId(Long conversationId);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId AND m.sender.id != :userId")
    long countMessagesNotFromUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.read = false")
    long countUnreadInConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    /**
     * Count total unread messages for a user across all conversations.
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.read = false AND m.sender.id != :userId " +
           "AND (m.conversation.buyer.id = :userId OR m.conversation.seller.id = :userId)")
    long countTotalUnread(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.read = false")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.conversation IN " +
           "(SELECT c FROM Conversation c JOIN c.paymentRelease pr " +
           "WHERE pr.paymentReleased = true AND pr.releasedAt < :cutoff)")
    int deleteExpiredMessages(@Param("cutoff") LocalDateTime cutoff);
}
