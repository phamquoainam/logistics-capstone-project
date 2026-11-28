#!/bin/sh
set -eu
# Neither password nor SQL results are printed. Both actual runtime DB and TLS are required.
export PGPASSFILE=/run/runtime/pgpass
psql -X -w -h postgres -U argopost_runtime -d "$(cat /run/runtime/database-name)" -qAt -c 'SELECT 1' >/dev/null 2>&1
curl --fail --silent --cacert /run/runtime/ca.crt https://localhost:8443/api/v1/public-media/hero-network.svg >/dev/null
