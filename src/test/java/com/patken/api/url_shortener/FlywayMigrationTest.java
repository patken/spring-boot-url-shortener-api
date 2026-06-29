package com.patken.api.url_shortener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the full application context against a throwaway in-memory database.
 */
@SpringBootTest(properties = {
        "DATASOURCE_URL=jdbc:h2:mem:flyway-validate;DB_CLOSE_DELAY=-1",
        "DDL_AUTO=validate"
})
@ActiveProfiles("local")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway applies V1 and Hibernate validates the schema on startup")
    void contextLoadsWithFlywayMigratedSchema() {
        var rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM url", Integer.class);
        assertEquals(0, rows, "the url table created by Flyway should exist and be empty");
    }

    @Test
    @DisplayName("Unique constraints from the migration are present on the url table")
    void uniqueConstraintsExist() {
        var uniqueConstraints = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                        "WHERE table_name = 'URL' AND constraint_type = 'UNIQUE'", Integer.class);
        assertTrue(uniqueConstraints != null && uniqueConstraints >= 2,
                "shorten_url and original_url should both be unique");
    }
}
