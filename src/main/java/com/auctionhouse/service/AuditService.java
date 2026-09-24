package com.auctionhouse.service;

import com.auctionhouse.model.AdminAuditLog;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.AdminAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AuditService — central recorder for administrative actions.
 *
 * Every admin mutation (user management, auctions, broadcasts, slides,
 * approvals, payments, support, settings) must be recorded here so the
 * Super Admin can see WHO did WHAT, to WHOM, and WHY — and filter the
 * log by admin, action type, and date range on /super-admin/audit-logs.
 *
 * Logging failures are swallowed (with a log statement) so that audit
 * problems can never break the actual admin operation.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AdminAuditLogRepository auditLogRepository;

    @Autowired
    public AuditService(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Full-control overload for non-user targets (auctions, broadcasts, slides, settings). */
    public void log(User performedBy, String action, Long targetUserId, String targetUsername, String details) {
        try {
            if (performedBy == null) {
                log.warn("Audit skipped (no performer): {} - {}", action, details);
                return;
            }
            String safeDetails = details;
            if (safeDetails != null && safeDetails.length() > 500) {
                safeDetails = safeDetails.substring(0, 497) + "...";
            }
            auditLogRepository.save(new AdminAuditLog(
                    action, targetUserId, targetUsername, performedBy, safeDetails));
        } catch (Exception e) {
            log.error("Failed to write audit log for action {}: {}", action, e.getMessage());
        }
    }

    /** Convenience overload when the target is a User. */
    public void log(User performedBy, String action, User target, String details) {
        log(performedBy, action,
                target != null ? target.getId() : null,
                target != null ? target.getUsername() : null,
                details);
    }

    /**
     * Filtered, paginated audit trail for the super-admin audit-logs page.
     * All parameters are optional — null means "don't filter on this".
     */
    public org.springframework.data.domain.Page<AdminAuditLog> getFilteredLogs(
            Long adminId, String action,
            java.time.LocalDateTime from, java.time.LocalDateTime to,
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<AdminAuditLog> spec =
                org.springframework.data.jpa.domain.Specification.where(null);

        if (adminId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("performedBy").get("id"), adminId));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("action"), action.trim()));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        return auditLogRepository.findAll(spec, pageable);
    }

    /** Admins who have performed at least one audited action (for the filter dropdown). */
    public java.util.List<User> getDistinctPerformers() {
        return auditLogRepository.findDistinctPerformers();
    }

    /** Action types recorded so far (for the filter dropdown). */
    public java.util.List<String> getDistinctActions() {
        return auditLogRepository.findDistinctActions();
    }
}
