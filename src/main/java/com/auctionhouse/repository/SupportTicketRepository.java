package com.auctionhouse.repository;

import com.auctionhouse.model.SupportTicket;
import com.auctionhouse.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    @Query("SELECT t FROM SupportTicket t WHERE t.user.id = :userId ORDER BY t.updatedAt DESC")
    List<SupportTicket> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT t FROM SupportTicket t WHERE t.user.id = :userId ORDER BY t.updatedAt DESC")
    Page<SupportTicket> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t ORDER BY t.updatedAt DESC")
    Page<SupportTicket> findAllOrderByUpdatedAtDesc(Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE t.status = :status ORDER BY t.updatedAt DESC")
    Page<SupportTicket> findByStatus(@Param("status") TicketStatus status, Pageable pageable);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = :status")
    long countByStatus(@Param("status") TicketStatus status);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status != 'CLOSED'")
    long countOpenTickets();

    @Query("SELECT t FROM SupportTicket t WHERE " +
           "LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.user.username) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.user.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY t.updatedAt DESC")
    Page<SupportTicket> searchTickets(@Param("q") String query, Pageable pageable);
}
