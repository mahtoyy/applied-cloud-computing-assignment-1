# Telecom Network Inventory

A small microservice application for keeping track of telecom **network sites** (towers, central offices, data centers) and the **equipment** installed at them (routers, switches, radios, firewalls). It is built for the *Applied Cloud Computing and Big Data* course (BTH) to demonstrate microservices on Kubernetes. All services are Spring Boot 4 / Java 21, with SQL Server as the database.

## Architecture

```
Browser ──► frontend ──► api-gateway ──┬──► site-service ──────► SQL Server: sitedb
                                       │        │  ▲
                                       │        ▼  │  (REST, internal)
                                       └──► equipment-service ─► SQL Server: equipmentdb
```

- `frontend`: server-rendered Thymeleaf UI, calls only the gateway.
- `api-gateway`: single entry point for the REST API, routes by path.
- `site-service`: owns sites; asks equipment-service before deleting a site.
- `equipment-service`: owns equipment; asks site-service that a site exists on create/move.
- SQL Server: one custom image (`db/Dockerfile`) that runs `init.sql` on every start (idempotent), two databases (`sitedb`, `equipmentdb`), one login per service, persistent volume.

The full design is in `docs/superpowers/specs/2026-10-23-network-inventory-design.md`.

## Prerequisites

Docker (Docker Desktop or Docker Engine with the Compose plugin). Nothing else is needed to run the app.

## Run locally

```bash
cp .env.example .env     # then edit .env: set three DIFFERENT passwords
docker compose up --build
```

Passwords need 8+ characters with upper case, lower case, a digit and a symbol (SQL Server policy). Avoid `$ " ' \ ;`.

| What | URL |
|---|---|
| UI | http://localhost:8080 |
| REST API (via gateway) | http://localhost:8081/api/sites |

Stop with `docker compose down`. Data is kept in the `mssql-data` volume; `docker compose down -v` deletes it.

Note: changing a password in `.env` after the volume exists does not update the existing database logins. Run `docker compose down -v` to re-initialize.

## Project layout

```
site-service/       Spring Boot service + Dockerfile
equipment-service/  Spring Boot service + Dockerfile
api-gateway/        Spring Cloud Gateway + Dockerfile
frontend/           Thymeleaf UI + Dockerfile
db/                 SQL Server image: Dockerfile, entrypoint.sh, init.sql (databases, logins)
docker-compose.yml  Local run of everything
k8s/                Kubernetes manifests (added separately)
docs/               Design spec and plans
```

## Tests

Run in each service directory:

```bash
cd site-service && ./mvnw test
```

(same for `equipment-service`, `api-gateway`, `frontend`). This needs a local JDK 21.

Troubleshooting: running a jar natively on Windows can fail with "Unable to establish loopback connection" when the temp path is an 8.3 short path (a user folder with a space). Workaround: set `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\tmp`. Docker is unaffected.
