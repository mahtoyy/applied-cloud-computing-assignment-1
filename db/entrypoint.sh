#!/bin/bash
# Starts SQL Server, runs init.sql once it succeeds, then keeps SQL Server in the foreground.
set -euo pipefail

SQLCMD=/opt/mssql-tools18/bin/sqlcmd
rm -f /tmp/init-done

/opt/mssql/bin/sqlservr &
SQL_PID=$!

# init.sql is idempotent, so retry it until SQL Server (and existing databases) finished starting.
echo "Running init.sql..."
done=0
for i in $(seq 1 30); do
  sleep 2
  if $SQLCMD -C -b -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -i /db/init.sql \
    -v SITE_DB_PASSWORD="$SITE_DB_PASSWORD" EQUIPMENT_DB_PASSWORD="$EQUIPMENT_DB_PASSWORD" >/tmp/init.log 2>&1; then
    done=1
    break
  fi
done
if [ "$done" -ne 1 ]; then
  echo "init.sql failed:" >&2
  cat /tmp/init.log >&2
  kill "$SQL_PID"
  exit 1
fi
touch /tmp/init-done
echo "Database initialisation done (attempt $i)."

wait "$SQL_PID"
