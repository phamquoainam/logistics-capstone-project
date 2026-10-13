-- Bootstrap runs only on the new empty volume, never on native PostgreSQL.
\getenv migrator_password ELOGISTICS_MIGRATOR_PASSWORD
\getenv runtime_password ELOGISTICS_RUNTIME_PASSWORD
CREATE ROLE elogistics_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE elogistics_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'migrator_password';
CREATE ROLE elogistics_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'runtime_password';
GRANT elogistics_owner TO elogistics_migrator;
CREATE DATABASE elogistics_dev OWNER elogistics_owner;
REVOKE ALL ON DATABASE elogistics_dev FROM PUBLIC;
GRANT CONNECT ON DATABASE elogistics_dev TO elogistics_migrator, elogistics_runtime;
\connect elogistics_dev
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA platform AUTHORIZATION elogistics_owner;
