package com.elogistic.backend.identity.internal.infrastructure;

import java.sql.*;
import javax.sql.DataSource;

/** Read-only startup requirement; never migrates, repairs or reads privileged Flyway history as runtime. */
public final class CustomerRuntimeRequirements {
    private CustomerRuntimeRequirements(){}
    public static void verify(DataSource source){
        try(Connection connection=source.getConnection();Statement query=connection.createStatement()){
            if(!connection.getMetaData().getDatabaseProductName().equals("PostgreSQL") || connection.getMetaData().getDatabaseMajorVersion()!=18)throw new IllegalStateException();
            query.setQueryTimeout(3);
            try(ResultSet row=query.executeQuery("SELECT to_regclass('identity.customer_enrollment') IS NOT NULL AND to_regclass('identity.customer_proof') IS NOT NULL AND to_regclass('identity.customer_delivery') IS NOT NULL AND to_regclass('identity.customer_command_receipt') IS NOT NULL AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='identity.rate_limit_attempt'::regclass AND conname='rate_limit_attempt_password_state')")){
                if(!row.next() || !row.getBoolean(1))throw new IllegalStateException();
            }
        }catch(Exception unavailable){throw new IllegalStateException("CUSTOMER_POSTGRES18_SCHEMA_V9_REQUIRED");}
    }
}
