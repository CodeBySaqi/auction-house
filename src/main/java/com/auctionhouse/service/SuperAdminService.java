package com.auctionhouse.service;

import com.auctionhouse.model.AdminAuditLog;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.AdminAuditLogRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * SuperAdminService - business logic for super admin management features.
 * Handles user/admin management with full security validation and audit logging.
 */
@Service
public class SuperAdminService {

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    /** Roles that only SUPER_ADMIN can assign. */
    private static final List<String> PROTECTED_ROLES = List.of(ROLE_SUPER_ADMIN, ROLE_ADMIN);

    private final UserRepository userRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SuperAdminService(UserRepository userRepository,
                              AdminAuditLogRepository auditLogRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== QUERIES ====================

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAllUsersOrderedByCreated() {
        return userRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> searchUsers(String query) {
        return userRepository.searchUsers(query);
    }

    public List<User> getAdmins() {
        return userRepository.findByRoleOrderByCreatedAtDesc(ROLE_ADMIN);
    }

    public List<User> getSuperAdmins() {
        return userRepository.findByRoleOrderByCreatedAtDesc(ROLE_SUPER_ADMIN);
    }

    public long countTotalUsers() {
        return userRepository.count();
    }

    public long countActiveUsers() {
        return userRepository.countByActive(true);
    }

    public long countDeactivatedUsers() {
        return userRepository.countByActive(false);
    }

    public long countAdmins() {
        return userRepository.countByRole(ROLE_ADMIN);
    }

    public long countSuperAdmins() {
        return userRepository.countByRole(ROLE_SUPER_ADMIN);
    }

    public List<AdminAuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<AdminAuditLog> getAuditLogsForUser(Long userId) {
        return auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
    }

    // ==================== USER ACTIVATION / DEACTIVATION ====================

    /**
     * Activate a user account.
     */
    @Transactional
    public void activateUser(Long userId, User performedBy) {
        User target = requireUser(userId);
        validateSuperAdmin(performedBy);
        preventSelfAction(target, performedBy, "activate");

        if (target.isActive()) {
            throw new IllegalStateException("User is already active.");
        }

        target.setActive(true);
        userRepository.save(target);

        audit("USER_ACTIVATED", target, performedBy,
                "Account activated: " + target.getUsername());
    }

    /**
     * Deactivate a user account. Does NOT delete data.
     */
    @Transactional
    public void deactivateUser(Long userId, User performedBy) {
        User target = requireUser(userId);
        validateSuperAdmin(performedBy);
        preventSelfAction(target, performedBy, "deactivate");
        preventSuperAdminModification(target);

        if (!target.isActive()) {
            throw new IllegalStateException("User is already deactivated.");
        }

        target.setActive(false);
        userRepository.save(target);

        audit("USER_DEACTIVATED", target, performedBy,
                "Account deactivated: " + target.getUsername() + " (role: " + target.getRole() + ")");
    }

    // ==================== ROLE MANAGEMENT ====================

    /**
     * Change a user's role. Only SUPER_ADMIN can call this.
     * Prevents:
     * - Self-modification
     * - Downgrading SUPER_ADMIN
     * - Assigning SUPER_ADMIN role to anyone (must use promoteToSuperAdmin)
     */
    @Transactional
    public void changeUserRole(Long userId, String newRole, User performedBy) {
        User target = requireUser(userId);
        validateSuperAdmin(performedBy);
        preventSelfAction(target, performedBy, "change role");
        preventSuperAdminModification(target);
        validateRoleAssignment(newRole, performedBy);

        String oldRole = target.getRole();
        if (oldRole.equals(newRole)) {
            throw new IllegalStateException("User already has role: " + newRole);
        }

        target.setRole(newRole);
        userRepository.save(target);

        audit("ROLE_CHANGED", target, performedBy,
                "Role changed from " + oldRole + " to " + newRole);
    }

    /**
     * Promote a user to ADMIN role.
     */
    @Transactional
    public void promoteToAdmin(Long userId, User performedBy) {
        User target = requireUser(userId);
        validateSuperAdmin(performedBy);
        preventSelfAction(target, performedBy, "promote");

        if (ROLE_SUPER_ADMIN.equals(target.getRole())) {
            throw new IllegalStateException("Cannot change a Super Admin's role through promotion.");
        }
        if (ROLE_ADMIN.equals(target.getRole())) {
            throw new IllegalStateException("User is already an Admin.");
        }

        String oldRole = target.getRole();
        target.setRole(ROLE_ADMIN);
        if (!target.isActive()) {
            target.setActive(true);
        }
        userRepository.save(target);

        audit("ADMIN_CREATED", target, performedBy,
                "Promoted from " + oldRole + " to ADMIN");
    }

    /**
     * Demote an admin back to USER role.
     */
    @Transactional
    public void demoteAdmin(Long userId, User performedBy) {
        User target = requireUser(userId);
        validateSuperAdmin(performedBy);
        preventSelfAction(target, performedBy, "demote");
        preventSuperAdminModification(target);

        if (!ROLE_ADMIN.equals(target.getRole())) {
            throw new IllegalStateException("User is not an Admin.");
        }

        target.setRole(ROLE_USER);
        userRepository.save(target);

        audit("ADMIN_DEMOTED", target, performedBy,
                "Demoted from ADMIN to USER");
    }

    // ==================== SECURITY VALIDATION ====================

    /**
     * Validate that the performer is a SUPER_ADMIN.
     */
    private void validateSuperAdmin(User performedBy) {
        if (performedBy == null || !ROLE_SUPER_ADMIN.equals(performedBy.getRole())) {
            throw new SecurityException("Only Super Admins can perform this action.");
        }
    }

    /**
     * Prevent a user from modifying their own account through management endpoints.
     */
    private void preventSelfAction(User target, User performedBy, String action) {
        if (target.getId().equals(performedBy.getId())) {
            throw new SecurityException("You cannot " + action + " your own account.");
        }
    }

    /**
     * Prevent any modification to SUPER_ADMIN accounts (except self-account changes by that super admin).
     */
    private void preventSuperAdminModification(User target) {
        if (ROLE_SUPER_ADMIN.equals(target.getRole())) {
            throw new SecurityException("Cannot modify a Super Admin account through this action.");
        }
    }

    /**
     * Validate that the role being assigned is allowed.
     * - Cannot assign SUPER_ADMIN through general role change
     * - Only ROLE_USER and ROLE_ADMIN are valid through changeUserRole
     */
    private void validateRoleAssignment(String newRole, User performedBy) {
        if (newRole == null || newRole.isEmpty()) {
            throw new IllegalArgumentException("Role cannot be empty.");
        }
        if (ROLE_SUPER_ADMIN.equals(newRole)) {
            throw new SecurityException("Cannot assign SUPER_ADMIN role through role change. Use the promote action.");
        }
        if (!ROLE_USER.equals(newRole) && !ROLE_ADMIN.equals(newRole)) {
            throw new IllegalArgumentException("Invalid role: " + newRole + ". Allowed: ROLE_USER, ROLE_ADMIN.");
        }
    }

    // ==================== HELPERS ====================

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void audit(String action, User target, User performedBy, String details) {
        AdminAuditLog log = new AdminAuditLog(
                action,
                target.getId(),
                target.getUsername(),
                performedBy,
                details
        );
        auditLogRepository.save(log);
    }

    /**
     * Ensure at least one SUPER_ADMIN exists. Called at startup.
     */
    @Transactional
    public void ensureSuperAdminExists() {
        long count = userRepository.countByRole(ROLE_SUPER_ADMIN);
        if (count == 0) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setEmail("superadmin@auctionhouse.com");
            superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setRole(ROLE_SUPER_ADMIN);
            superAdmin.setWalletBalance(100000.0);
            superAdmin.setActive(true);
            userRepository.save(superAdmin);
            System.out.println("=== SUPER ADMIN CREATED ===");
            System.out.println("Username: superadmin");
            System.out.println("Password: superadmin123");
            System.out.println("===========================");
        }
    }
}
