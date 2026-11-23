package com.elogistic.backend.testsupport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** AON Option B prerequisites only, called by the existing owned privileged bootstrap.
 * No operator login/membership, runtime role changes or developer connection.
 */
final class AdminRoleTestBootstrap {
    private AdminRoleTestBootstrap() {}
    static void roles(Connection admin, String owner) throws SQLException {
        if (!owner.matches("owner_[a-f0-9]{32}")) throw new IllegalArgumentException("OWNED_AON_OWNER_REQUIRED");
        boolean original = admin.getAutoCommit();
        admin.setAutoCommit(false);
        try (var s = admin.createStatement()) {
            s.execute("SELECT pg_advisory_xact_lock(730039)");
            for (String role : List.of("elogistics_local_admin_operator", "identity_admin_writer", "elogistics_local_realm_operator")) {
                boolean exists;
                try (var r = s.executeQuery("SELECT rolcanlogin,rolsuper,rolcreatedb,rolcreaterole,rolreplication,rolbypassrls FROM pg_roles WHERE rolname='" + role + "'")) {
                    exists = r.next();
                    if (exists) for (int i = 1; i <= 6; i++) if (r.getBoolean(i)) throw new SQLException("UNSAFE_AON_PREREQUISITE", "42501");
                }
                if (!exists) s.execute("CREATE ROLE " + role + " NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS");
            }
            s.execute("GRANT identity_admin_writer TO " + owner + " WITH INHERIT FALSE, SET TRUE");
            admin.commit();
        } catch (SQLException | RuntimeException failure) {
            admin.rollback(); throw failure;
        } finally { admin.setAutoCommit(original); }
    }
    static void database(Connection admin, String owner) throws SQLException {
        if (!owner.matches("owner_[a-f0-9]{32}")) throw new IllegalArgumentException("OWNED_AON_OWNER_REQUIRED");
        try (var s = admin.createStatement()) {
            // AON-ACL-02: catalog ACL is per freshly owned test database.
            s.execute("REVOKE EXECUTE ON FUNCTION pg_catalog.pg_control_system() FROM PUBLIC");
            s.execute("GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system() TO identity_admin_writer," + owner);
        }
    }
}
