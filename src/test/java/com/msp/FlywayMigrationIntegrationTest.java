package com.msp;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@ActiveProfiles("prod")
@ContextConfiguration(initializers = FlywayMigrationIntegrationTest.DatabaseInitializer.class)
class FlywayMigrationIntegrationTest {

    private static final String JDBC_URL = env("FLYWAY_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/msp_flyway_test");
    private static final String USERNAME = env("FLYWAY_TEST_USERNAME", "postgres");
    private static final String PASSWORD = env("FLYWAY_TEST_PASSWORD", "12345");

    static boolean databaseReady;

    static class DatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            databaseReady = prepareFreshDatabaseAndMigrate();
            assumeTrue(databaseReady, "Local PostgreSQL is not available on localhost:5432");
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", () -> JDBC_URL);
        registry.add("DB_USERNAME", () -> USERNAME);
        registry.add("DB_PASSWORD", () -> PASSWORD);
        registry.add("JWT_SECRET", () -> "test-jwt-secret-key-for-flyway-integration");
        registry.add("REDIS_HOST", () -> "127.0.0.1");
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
    }

    @Test
    void prodProfileStartsAfterFlywayMigrations() throws Exception {
        assumeTrue(databaseReady, "Local PostgreSQL is not available on localhost:5432");

        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            assertTableExists(statement, "admin_consent_requests");
            assertTableExists(statement, "audit_logs");

            ResultSet history = statement.executeQuery(
                    "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1");
            history.next();
            assertEquals("16", history.getString(1));
        }
    }

    private static boolean prepareFreshDatabaseAndMigrate() {
        if (!canConnect("jdbc:postgresql://localhost:5432/postgres", USERNAME, PASSWORD)) {
            return false;
        }

        try {
            try (Connection admin = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", USERNAME, PASSWORD);
                 Statement statement = admin.createStatement()) {
                statement.executeUpdate("DROP DATABASE IF EXISTS msp_flyway_test");
                statement.executeUpdate("CREATE DATABASE msp_flyway_test");
            }

            Flyway flyway = Flyway.configure()
                    .dataSource(JDBC_URL, USERNAME, PASSWORD)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load();
            flyway.migrate();
            return true;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to prepare Flyway test database", ex);
        }
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static boolean canConnect(String url, String user, String password) {
        try (Connection ignored = DriverManager.getConnection(url, user, password)) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void assertTableExists(Statement statement, String tableName) throws Exception {
        ResultSet result = statement.executeQuery(
                "SELECT to_regclass('public." + tableName + "')");
        result.next();
        assertNotNull(result.getString(1), "Expected table to exist: " + tableName);
    }
}
