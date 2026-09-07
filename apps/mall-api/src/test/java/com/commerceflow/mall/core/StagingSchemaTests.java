package com.commerceflow.mall.core;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;

class StagingSchemaTests {
    private String database() {
        return "jdbc:h2:mem:staging_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    }

    private Flyway migration(String url) {
        return Flyway.configure().dataSource(url, "sa", "")
                .locations("classpath:db/staging-migration")
                .table("commerceflow_schema_history")
                .baselineOnMigrate(false).cleanDisabled(true).load();
    }

    @Test
    void freshStagingSchemaIsEmptyAndRetainsOrderAndIdentityContracts() throws Exception {
        String url = database();
        Flyway flyway = migration(url);
        assertEquals(1, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var query = connection.createStatement()) {
            for (String table : new String[]{"user_account", "product", "product_sku", "inventory",
                    "orders", "order_item", "ai_trace", "inventory_movement", "outbox_event"}) {
                try (var rows = query.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    assertTrue(rows.next());
                    assertEquals(0, rows.getInt(1), table + " must not contain Demo data");
                }
            }
            query.executeQuery("SELECT external_subject FROM user_account").close();
            query.executeQuery("SELECT product_code, cover_image_path FROM product").close();
            query.executeQuery("SELECT color_snapshot, size_snapshot, image_path_snapshot FROM order_item").close();
            assertThrows(SQLException.class, () -> query.executeUpdate(
                    "INSERT INTO inventory (sku_id, available_stock) VALUES (1, -1)"));
        }
    }

    @Test
    void refusesToAdoptAnExistingDatabaseWithoutAnExplicitMigrationPlan() throws Exception {
        String url = database();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var query = connection.createStatement()) {
            query.execute("CREATE TABLE existing_business_data (id INT PRIMARY KEY)");
            query.execute("INSERT INTO existing_business_data VALUES (42)");
            assertThrows(FlywayException.class, () -> migration(url).migrate());
            try (var rows = query.executeQuery("SELECT id FROM existing_business_data")) {
                assertTrue(rows.next());
                assertEquals(42, rows.getInt(1));
            }
        }
    }
}
