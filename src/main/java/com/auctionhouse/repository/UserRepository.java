package com.auctionhouse.repository;

import com.auctionhouse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /** Pessimistic lock on user row to prevent concurrent wallet corruption. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    /* ---------- Admin console additions ---------- */

    long countByRole(String role);

    /** Usernames starting with a prefix, used for the admin search box. */
    List<User> findTop20ByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(String fragment);

    long countByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT COALESCE(SUM(u.walletBalance), 0) FROM User u")
    double sumWalletBalance();

    /* ---------- Super Admin additions ---------- */

    List<User> findByRole(String role);

    List<User> findByRoleOrderByCreatedAtDesc(String role);

    long countByActive(boolean active);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY u.createdAt DESC")
    List<User> searchUsers(@org.springframework.data.repository.query.Param("q") String query);
}
