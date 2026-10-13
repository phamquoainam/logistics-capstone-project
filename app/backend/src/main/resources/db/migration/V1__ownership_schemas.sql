-- Global PostgreSQL version stream. Bootstrap creates platform/history prerequisite.
-- Flyway SET ROLE selects the NOLOGIN database owner; runtime never owns objects.
CREATE SCHEMA identity;
CREATE SCHEMA merchant;
CREATE SCHEMA orders;
CREATE SCHEMA pricing;
CREATE SCHEMA warehouse;
CREATE SCHEMA dispatch;
CREATE SCHEMA finance;
CREATE SCHEMA routing;
CREATE SCHEMA notification;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA platform FROM PUBLIC;
GRANT USAGE ON SCHEMA identity, merchant, orders TO "${runtimeRole}";
