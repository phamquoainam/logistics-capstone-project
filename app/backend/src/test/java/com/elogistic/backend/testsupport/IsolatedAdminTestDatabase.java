package com.elogistic.backend.testsupport;

import java.sql.DriverManager;
import java.util.UUID;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** A fresh, exactly named AON database inside the existing owned Task-C container only. */
public final class IsolatedAdminTestDatabase implements AutoCloseable {
    private final PostgresTestcontainersSupport.Database database;
    private IsolatedAdminTestDatabase(PostgresTestcontainersSupport.Database database){this.database=database;}
    public static IsolatedAdminTestDatabase create(PostgreSQLContainer container)throws Exception {
        PostgresTestcontainersSupport.verifyRuntime(container);
        var parent=PostgresTestcontainersSupport.database(container);
        String name="argopost_admin_uat_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(container,"postgres"),container.getUsername(),container.getPassword());var s=c.createStatement()) {
            s.execute("CREATE DATABASE "+name+" OWNER "+parent.owner());
            s.execute("REVOKE ALL ON DATABASE "+name+" FROM PUBLIC");
            s.execute("GRANT CONNECT ON DATABASE "+name+" TO "+parent.migrator()+","+parent.runtime());
        }
        var db=new PostgresTestcontainersSupport.Database(name,parent.owner(),parent.migrator(),parent.runtime(),parent.migrationPassword(),parent.runtimePassword(),container);
        try(var c=DriverManager.getConnection(db.url(),container.getUsername(),container.getPassword());var s=c.createStatement()) {
            s.execute("REVOKE ALL ON SCHEMA public FROM PUBLIC");s.execute("CREATE SCHEMA platform AUTHORIZATION "+db.owner());
            StaffRoleTestBootstrap.database(c,db.owner());
        }
        return new IsolatedAdminTestDatabase(db);
    }
    public PostgresTestcontainersSupport.Database database(){return database;}
    @Override public void close()throws Exception {
        var pg=database.container();PostgresTestcontainersSupport.verifyRuntime(pg);
        if(!database.name().matches("argopost_admin_uat_[a-f0-9]{12}")||!database.owner().matches("owner_[a-f0-9]{32}"))throw new IllegalStateException("AON_FIXTURE_IDENTITY_INVALID");
        try(var c=DriverManager.getConnection(PostgresTestcontainersSupport.currentUrl(pg,"postgres"),pg.getUsername(),pg.getPassword());var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT pg_get_userbyid(datdba) FROM pg_database WHERE datname='"+database.name()+"'")) {
                if(!r.next()||!database.owner().equals(r.getString(1)))throw new IllegalStateException("AON_FIXTURE_OWNER_CHANGED");
            }
            // No FORCE or connection termination: foreign/open users remain a hard failure.
            s.execute("DROP DATABASE "+database.name());
        }
    }
}
