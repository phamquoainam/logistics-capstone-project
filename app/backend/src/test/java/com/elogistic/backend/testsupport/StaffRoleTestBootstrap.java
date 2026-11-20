package com.elogistic.backend.testsupport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Owner-approved ACL-05 prerequisite, called only by an owned test cluster administrator. */
final class StaffRoleTestBootstrap {
    private static final List<String> ROLES = List.of(
        "elogistics_local_staff_operator", "identity_staff_writer", "support_grant_writer");

    private StaffRoleTestBootstrap() {}

    static void roles(Connection admin, String owner) throws SQLException {
        if (!owner.matches("owner_[a-f0-9]{32}")) throw new IllegalArgumentException("Unknown isolated owner");
        boolean autoCommit = admin.getAutoCommit();
        admin.setAutoCommit(false);
        try (var statement = admin.createStatement()) {
            // Cluster-scoped role names are shared by databases of this owned fixture only.
            statement.execute("SELECT pg_advisory_xact_lock(730036)");
            for (String role : ROLES) {
                boolean exists;
                try (var rows = statement.executeQuery("SELECT rolcanlogin,rolsuper,rolcreatedb,rolcreaterole,rolreplication,rolbypassrls FROM pg_roles WHERE rolname='" + role + "'")) {
                    exists = rows.next();
                    if (exists) for (int i = 1; i <= 6; i++)
                        if (rows.getBoolean(i)) throw new SQLException("Unsafe existing Staff role prerequisite", "42501");
                }
                if (!exists) statement.execute("CREATE ROLE " + role + " NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS");
            }
            // Only the trusted migration owner can SET for ownership transfer; no inherited DML.
            statement.execute("GRANT identity_staff_writer,support_grant_writer TO " + owner + " WITH INHERIT FALSE, SET TRUE");
            admin.commit();
        } catch (SQLException | RuntimeException failure) {
            admin.rollback();
            throw failure;
        } finally {
            admin.setAutoCommit(autoCommit);
        }
    }

    static void database(Connection admin, String owner) throws SQLException {
        if (!owner.matches("owner_[a-f0-9]{32}")) throw new IllegalArgumentException("Unknown isolated owner");
        try (var statement = admin.createStatement()) {
            statement.execute("GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO identity_staff_writer," + owner);
        }
    }
}
