package com.auctionhouse.repository;

import com.auctionhouse.model.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    @Query("SELECT m FROM TicketMessage m WHERE m.ticket.id = :ticketId ORDER BY m.createdAt ASC")
    List<TicketMessage> findByTicketId(@Param("ticketId") Long ticketId);

    @Modifying
    @Query("UPDATE TicketMessage m SET m.isRead = true WHERE m.ticket.id = :ticketId AND m.sender.id != :readerId AND m.isRead = false")
    int markAsRead(@Param("ticketId") Long ticketId, @Param("readerId") Long readerId);

    @Query("SELECT COUNT(m) FROM TicketMessage m WHERE m.ticket.id = :ticketId AND m.sender.id != :userId AND m.isRead = false")
    long countUnread(@Param("ticketId") Long ticketId, @Param("userId") Long userId);
}
