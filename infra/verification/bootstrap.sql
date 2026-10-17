-- Verification-only provisioning; production Flyway V1/V2 are unchanged.
\getenv database VERIFY_DATABASE
\getenv owner VERIFY_OWNER
\getenv migrator VERIFY_MIGRATOR
\getenv runtime VERIFY_RUNTIME
\getenv migrator_password VERIFY_MIGRATOR_PASSWORD
\getenv runtime_password VERIFY_RUNTIME_PASSWORD
CREATE ROLE :"owner" NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE :"migrator" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'migrator_password';
CREATE ROLE :"runtime" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'runtime_password';
GRANT :"owner" TO :"migrator";
CREATE DATABASE :"database" OWNER :"owner";
REVOKE ALL ON DATABASE :"database" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"database" TO :"migrator", :"runtime";
\connect :database
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA platform AUTHORIZATION :"owner";
