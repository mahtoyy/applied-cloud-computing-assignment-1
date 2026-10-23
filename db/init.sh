#!/bin/bash
set -euo pipefail

SQLCMD=/opt/mssql-tools18/bin/sqlcmd

echo "Waiting for SQL Server at $DB_HOST..."
ready=0
for i in $(seq 1 30); do
  if $SQLCMD -C -S "$DB_HOST" -U sa -P "$SA_PASSWORD" -Q "SELECT 1" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done
if [ "$ready" -ne 1 ]; then
  echo "SQL Server did not answer in time" >&2
  exit 1
fi

echo "Running init.sql..."
$SQLCMD -C -b -S "$DB_HOST" -U sa -P "$SA_PASSWORD" -i /db/init.sql \
  -v SITE_DB_PASSWORD="$SITE_DB_PASSWORD" EQUIPMENT_DB_PASSWORD="$EQUIPMENT_DB_PASSWORD"
echo "Database initialisation done."
