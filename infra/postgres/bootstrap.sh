#!/bin/sh
set -eu
ELOGISTICS_MIGRATOR_PASSWORD="$(cat /run/secrets/migrator_password)"
ELOGISTICS_RUNTIME_PASSWORD="$(cat /run/secrets/runtime_password)"
export ELOGISTICS_MIGRATOR_PASSWORD ELOGISTICS_RUNTIME_PASSWORD
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -f /opt/elogistics/bootstrap.sql
unset ELOGISTICS_MIGRATOR_PASSWORD ELOGISTICS_RUNTIME_PASSWORD
