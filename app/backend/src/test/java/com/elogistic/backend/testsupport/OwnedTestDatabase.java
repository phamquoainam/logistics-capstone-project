package com.elogistic.backend.testsupport;

import com.elogistic.backend.testsupport.PostgresTestcontainersSupport.Database;
import java.sql.DriverManager;

/** Releases only an exact completed factory fixture inside its still-owned test container.
 * No container/volume/role deletion, FORCE, session termination or developer URL.
 */
public final class OwnedTestDatabase {
    private OwnedTestDatabase() {}

    public static void release(Database db) throws Exception {
        if (db == null || db.name() == null || !db.name().matches("task_c_[a-f0-9]{32}"))
            throw new IllegalArgumentException("OWNED_TEST_DATABASE_REQUIRED");
        String suffix = db.name().substring("task_c_".length());
        if (!("owner_" + suffix).equals(db.owner()) || !("migrator_" + suffix).equals(db.migrator())
                || !("runtime_" + suffix).equals(db.runtime()))
            throw new IllegalArgumentException("OWNED_TEST_DATABASE_BINDING_REQUIRED");
        // Reuse the complete accepted container/volume/run/session ownership proof.
        PostgresTestcontainersSupport.verifyRuntime(db.container());
        // Also re-resolve the current loopback-only mapped port, never a developer URL.
        String adminUrl = PostgresTestcontainersSupport.currentUrl(db.container(), "postgres");
        try (var admin = DriverManager.getConnection(adminUrl, db.container().getUsername(), db.container().getPassword())) {
            if (admin.getMetaData().getDatabaseMajorVersion() != 18)
                throw new IllegalStateException("OWNED_POSTGRES18_REQUIRED");
            try (var query = admin.prepareStatement("SELECT pg_get_userbyid(datdba) FROM pg_database WHERE datname=?")) {
                query.setString(1, db.name());
                try (var rows = query.executeQuery()) {
                    if (!rows.next()) return; // Exact completed-fixture cleanup replay.
                    if (!db.owner().equals(rows.getString(1)))
                        throw new IllegalStateException("TEST_DATABASE_OWNER_CHANGED");
                }
            }
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
            while (true) {
                try (var query = admin.prepareStatement("SELECT count(*) FROM pg_stat_activity WHERE datname=?")) {
                    query.setString(1, db.name());
                    try (var rows = query.executeQuery()) {
                        if (!rows.next()) throw new IllegalStateException("TEST_DATABASE_ACTIVITY_UNAVAILABLE");
                        if (rows.getInt(1) == 0) break;
                    }
                }
                if (System.nanoTime() >= deadline) throw new IllegalStateException("TEST_DATABASE_STILL_CONNECTED");
                try { Thread.sleep(25); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw interrupted; }
            }
            // Recheck ownership after waiting; a concurrent owner change must retain the database.
            try (var query = admin.prepareStatement("SELECT pg_get_userbyid(datdba) FROM pg_database WHERE datname=?")) {
                query.setString(1, db.name());
                try (var rows = query.executeQuery()) {
                    if (!rows.next()) return;
                    if (!db.owner().equals(rows.getString(1))) throw new IllegalStateException("TEST_DATABASE_OWNER_CHANGED");
                }
            }
            // Identifiers have the exact factory grammar and matching physical catalog owner.
            // PostgreSQL itself also rejects any connection racing this non-FORCE drop.
            try (var statement = admin.createStatement()) {
                statement.execute("DROP DATABASE " + db.name());
            }
        }
    }
}
