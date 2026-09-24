package com.auctionhouse.repository;

import com.auctionhouse.model.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long>, JpaSpecificationExecutor<AdminAuditLog> {

    List<AdminAuditLog> findAllByOrderByCreatedAtDesc();

    List<AdminAuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    List<AdminAuditLog> findByPerformedByIdOrderByCreatedAtDesc(Long performedById);

    /** Distinct admins who appear in the audit trail — for the "filter by admin" dropdown. */
    @Query("SELECT DISTINCT a.performedBy FROM AdminAuditLog a ORDER BY a.performedBy.username ASC")
    List<com.auctionhouse.model.User> findDistinctPerformers();

    /** Distinct action types recorded — for the "filter by action" dropdown. */
    @Query("SELECT DISTINCT a.action FROM AdminAuditLog a ORDER BY a.action ASC")
    List<String> findDistinctActions();
}
