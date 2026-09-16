package com.auctionhouse.repository;

import com.auctionhouse.model.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    
    List<AdminAuditLog> findAllByOrderByCreatedAtDesc();
    
    List<AdminAuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    
    List<AdminAuditLog> findByPerformedByIdOrderByCreatedAtDesc(Long performedById);
}
