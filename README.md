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

## Run on Kubernetes

Manifests are in `k8s/` and are applied with Kustomize (built into `kubectl`). Tested on Docker Desktop Kubernetes.

1. Build and push the images to Docker Hub (replace `<user>` with your Docker Hub username):

   ```bash
   docker compose build
   for i in netinventory-db site-service equipment-service api-gateway frontend; do
     docker tag $i:dev <user>/$i:1.0 && docker push <user>/$i:1.0
   done
   ```

2. In `k8s/kustomization.yaml`, replace `DOCKERHUB_USER` with your username.
3. Create the namespace and the password Secret from your `.env` (the Secret is not stored in git):

   ```bash
   kubectl apply -f k8s/namespace.yaml
   kubectl create secret generic db-credentials -n netinventory --from-env-file=.env
   ```

4. Deploy everything and wait until it is ready:

   ```bash
   kubectl apply -k k8s
   kubectl get pods -n netinventory -w
   ```

The UI is at http://localhost:8080 and the REST API at http://localhost:8081/api/sites (LoadBalancer services). On the first start, site-service and equipment-service restart a couple of times until the database is ready. That is expected: Kubernetes restarts them until they connect.

| File | What it creates |
|---|---|
| `namespace.yaml` | `netinventory` namespace |
| `config.yaml` | ConfigMap with database URLs, service URLs and JVM options |
| `db.yaml` | SQL Server StatefulSet (1 replica) with a 2 Gi PersistentVolumeClaim, and a headless Service `db` |
| `site-service.yaml`, `equipment-service.yaml` | Deployment (2 replicas) + internal ClusterIP Service |
| `api-gateway.yaml`, `frontend.yaml` | Deployment (2 replicas) + LoadBalancer Service (ports 8081 and 8080) |
| `network-policies.yaml` | Deny all incoming traffic, then allow only frontend → gateway → services → db |
| `kustomization.yaml` | Namespace and Docker Hub image names for all of the above |

Every Spring Boot pod has startup, liveness and readiness probes on `/actuator/health/*`. Readiness for site-service and equipment-service includes the database connection. The db pod is ready once `init.sql` has run.

Useful commands for the demo:

```bash
kubectl scale deploy/site-service -n netinventory --replicas=3   # scale one service on its own
kubectl logs -n netinventory deploy/site-service                 # service logs
kubectl delete pod db-0 -n netinventory                          # data survives: it is on the PVC
kubectl delete namespace netinventory                            # remove everything, including the data
```

## REST API

All endpoints are reached through the gateway. Errors return `{status, error, message}`.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/sites` | List sites |
| GET | `/api/sites/{id}` | 404 if missing |
| POST / PUT | `/api/sites`, `/api/sites/{id}` | 409 if the site code already exists |
| DELETE | `/api/sites/{id}` | 409 if the site still has equipment |
| GET | `/api/equipment?siteId={id}` | List equipment, optionally for one site |
| GET | `/api/equipment/count?siteId={id}` | Used by site-service before delete |
| GET | `/api/equipment/{id}` | 404 if missing |
| POST / PUT | `/api/equipment`, `/api/equipment/{id}` | 400 if the site does not exist, 409 on duplicate hostname |
| DELETE | `/api/equipment/{id}` | |

If the other service is down during a cross-service check, the call returns 503.

## Project layout

```
site-service/       Spring Boot service + Dockerfile
equipment-service/  Spring Boot service + Dockerfile
api-gateway/        Spring Cloud Gateway + Dockerfile
frontend/           Thymeleaf UI + Dockerfile
db/                 SQL Server image: Dockerfile, entrypoint.sh, init.sql (databases, logins)
docker-compose.yml  Local run of everything
k8s/                Kubernetes manifests (Kustomize)
```

Inside `site-service` and `equipment-service` the code is split into packages: `controller` (REST), `service` (business rules), `repository` (Spring Data JPA), `model` (entities, enums), `dto` (request/response bodies), `client` (calls to the other service) and `error` (exceptions and error responses). The frontend has `controller`, `advice`, `client` (gateway client) and `model`. Database schema and seed data are Flyway migrations in `src/main/resources/db/migration`.

## Tests

Run in each service directory:

```bash
cd site-service && ./mvnw test
```

(same for `equipment-service`, `api-gateway`, `frontend`). This needs a local JDK 21.

Troubleshooting: running a jar natively on Windows can fail with "Unable to establish loopback connection" when the temp path is an 8.3 short path (a user folder with a space). Workaround: set `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\tmp`. Docker is unaffected.
