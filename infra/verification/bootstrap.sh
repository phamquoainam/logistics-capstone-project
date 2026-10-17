#!/bin/sh
set -eu
VERIFY_MIGRATOR_PASSWORD="$(cat /run/secrets/pg_migrator)"
VERIFY_RUNTIME_PASSWORD="$(cat /run/secrets/pg_runtime)"
export VERIFY_MIGRATOR_PASSWORD VERIFY_RUNTIME_PASSWORD
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -f /opt/verification/bootstrap.sql
unset VERIFY_MIGRATOR_PASSWORD VERIFY_RUNTIME_PASSWORD
