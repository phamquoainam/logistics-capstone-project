#!/bin/sh
set -eu
# Called only by official postgres entrypoint on a NEW Compose-owned volume.
# Suppress SQL diagnostics: bootstrap SQL contains generated role passwords.
if ! psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -f /run/database/bootstrap.sql >/tmp/argopost-bootstrap.log 2>&1; then
    echo 'ARGOPOST_LOCAL_BOOTSTRAP_FAILED; inspect protected container diagnostic, no reset performed' >&2
    exit 1
fi
echo 'ARGOPOST_LOCAL_DATABASE_ROLES_READY'
