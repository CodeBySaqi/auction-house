package com.auctionhouse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Runs one-time schema migrations on startup.
 * Hibernate ddl-auto=update can ADD columns but cannot loosen NOT NULL constraints.
 * This component handles those cases for the DM (direct messaging) feature.
 */
@Component
public class SchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final DataSource dataSource;

    @Autowired
    public SchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Migration: Make auction_id nullable in conversations (for DM support)
            // Hibernate ddl-auto=update added the column as NOT NULL originally,
            // but DM conversations don't have an auction.
            try {
                stmt.execute("ALTER TABLE conversations ALTER COLUMN auction_id DROP NOT NULL");
                log.info("Migration: conversations.auction_id is now nullable");
            } catch (Exception e) {
                // Already nullable or syntax differs — not an error
                log.debug("auction_id nullable check: {}", e.getMessage());
            }

            // Migration: Make payment_release_id nullable in conversations (for DM support)
            try {
                stmt.execute("ALTER TABLE conversations ALTER COLUMN payment_release_id DROP NOT NULL");
                log.info("Migration: conversations.payment_release_id is now nullable");
            } catch (Exception e) {
                log.debug("payment_release_id nullable check: {}", e.getMessage());
            }

            // Migration: Drop unique constraint on payment_release_id if it prevents
            // multiple NULL values (some DB engines don't allow multiple NULLs in unique cols)
            // H2 allows multiple NULLs in unique columns, so this is a no-op for H2.

        } catch (Exception e) {
            log.warn("Schema migration check failed (non-fatal): {}", e.getMessage());
        }
    }
}
