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
    
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.conversation IN " +
           "(SELECT c FROM Conversation c JOIN c.paymentRelease pr " +
           "WHERE pr.paymentReleased = true AND pr.releasedAt < :cutoff)")
    int deleteExpiredMessages(@Param("cutoff") LocalDateTime cutoff);
}
