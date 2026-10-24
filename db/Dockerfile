FROM mcr.microsoft.com/mssql/server:2022-latest

COPY --chmod=755 entrypoint.sh /db/entrypoint.sh
COPY --chmod=644 init.sql /db/init.sql

ENV ACCEPT_EULA=Y

# Healthy only after init.sql has run in this container start.
HEALTHCHECK --interval=5s --timeout=5s --start-period=20s --retries=30 \
  CMD test -f /tmp/init-done && /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -Q "SELECT 1" || exit 1

ENTRYPOINT ["/bin/bash", "/db/entrypoint.sh"]
