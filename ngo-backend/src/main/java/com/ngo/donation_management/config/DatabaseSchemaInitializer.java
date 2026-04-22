package com.ngo.donation_management.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alignSchema() {
        addDonorPhoneColumnIfMissing();
        addUserNgoColumnIfMissing();
        addUrgentNeedNgoColumnIfMissing();
        addPickupRejectionReasonColumnIfMissing();
        addDonationRequestRejectionReasonColumnIfMissing();
        jdbcTemplate.execute(
                "ALTER TABLE donation MODIFY COLUMN campaign_id INT NULL"
        );
        jdbcTemplate.execute(
                "ALTER TABLE donation MODIFY COLUMN donation_status " +
                        "ENUM('pending','approved','completed','cancelled') NOT NULL"
        );
        jdbcTemplate.execute(
                "ALTER TABLE pickup_request MODIFY COLUMN pickup_status " +
                        "ENUM('awaiting_approval','pending','assigned','completed','cancelled') NOT NULL"
        );
        jdbcTemplate.execute(
                "ALTER TABLE volunteer MODIFY COLUMN ngo_id INT NULL"
        );
    }

    private void addDonorPhoneColumnIfMissing() {
        Integer columnCount = getColumnCount("pickup_request", "donor_phone");

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE pickup_request ADD COLUMN donor_phone VARCHAR(20) NULL"
            );
        }
    }

    private void addUserNgoColumnIfMissing() {
        Integer columnCount = getColumnCount("user", "ngo_id");

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE user ADD COLUMN ngo_id INT NULL"
            );
        }
    }

    private void addUrgentNeedNgoColumnIfMissing() {
        Integer columnCount = getColumnCount("urgent_needs", "ngo_id");

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE urgent_needs ADD COLUMN ngo_id INT NULL"
            );
        }
    }

    private void addPickupRejectionReasonColumnIfMissing() {
        Integer columnCount = getColumnCount("pickup_request", "rejection_reason");

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE pickup_request ADD COLUMN rejection_reason TEXT NULL"
            );
        }
    }

    private void addDonationRequestRejectionReasonColumnIfMissing() {
        Integer columnCount = getColumnCount("donation_request", "rejection_reason");

        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE donation_request ADD COLUMN rejection_reason TEXT NULL"
            );
        }
    }

    private Integer getColumnCount(String tableName, String columnName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? " +
                        "AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
    }
}
