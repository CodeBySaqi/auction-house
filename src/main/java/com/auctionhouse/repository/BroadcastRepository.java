package com.auctionhouse.repository;

import com.auctionhouse.model.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {

    @Query("SELECT b FROM Broadcast b WHERE b.scheduled = false ORDER BY b.sentAt DESC")
    List<Broadcast> findSentBroadcasts();

    @Query("SELECT b FROM Broadcast b WHERE b.scheduled = true ORDER BY b.scheduledFor ASC")
    List<Broadcast> findScheduledBroadcasts();

    long countByScheduled(boolean scheduled);

    @Query("SELECT COALESCE(SUM(b.recipientCount), 0) FROM Broadcast b WHERE b.scheduled = false")
    long sumDeliveredMessages();
}
