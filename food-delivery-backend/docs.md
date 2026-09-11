# Food Delivery Backend — Local Docker & K8s Guide

## Best practice: Docker Compose first, then Kubernetes

| Stage | Tool | Purpose |
|-------|------|---------|
| Develop & verify | **Docker Compose** | Run the full stack locally, fix bugs, test login/cart/order/pay |
| Deploy like production | **Kubernetes** | Pods, Services, scaling, health checks |

**Do this in order:**

1. Get everything healthy with Docker Compose.
2. Manually test the app (auth, restaurants, cart, orders, payments).
3. Fix issues while still on Compose.
4. Stop Compose (`docker compose down`).
5. Deploy to K8s and smoke-test the same flows.

**Do not run Compose and K8s at the same time** on one machine unless you intentionally use different ports. Both will fight over `3306`, `8080`, etc.

---

## MySQL (Docker Compose)

The MySQL used by this project is the **Docker container** `food_delivery_mysql` — not a separately installed Mac MySQL.

### Connection details

| Field | Value |
|-------|--------|
| Container name | `food_delivery_mysql` |
| Compose service name | `mysql-db` |
| User | `root` |
| Password | `Vishal@90` |
| Default DB (created at start) | `auth_db` |

### Which host to use

| You connect from | Host | Port |
|------------------|------|------|
| Mac apps (Workbench, DBeaver, local CLI) | `127.0.0.1` or `localhost` | `3306` |
| Other Docker containers (auth, order, …) | `mysql-db` | `3306` |

**JDBC from Mac:**

```text
jdbc:mysql://localhost:3306/auth_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

**JDBC from another container:**

```text
jdbc:mysql://mysql-db:3306/auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

### Important: MySQL is not a website

Opening `http://mysql-db:3306/` or `http://localhost:3306/` in a browser **will never work**.

- Browsers speak HTTP; MySQL does not.
- `mysql-db` only resolves **inside** the Docker network, not on your Mac.

Use Workbench, DBeaver, or the CLI instead.

### View databases (CLI)

```bash
docker exec -it food_delivery_mysql mysql -uroot -p'Vishal@90' -e "SHOW DATABASES;"
```

Or interactive shell:

```bash
docker exec -it food_delivery_mysql mysql -uroot -p'Vishal@90'
```

Then:

```sql
SHOW DATABASES;
SELECT VERSION();
```

### Connect with MySQL Workbench

1. Open MySQL Workbench → **+** next to MySQL Connections.
2. Set:
   - Hostname: `127.0.0.1`
   - Port: `3306`
   - Username: `root`
   - Password: `Vishal@90` (store in keychain)
3. **Test Connection** → OK → open the connection.
4. Use the left **SCHEMAS** panel to browse databases and tables.

### Connect with DBeaver

1. **Database → New Database Connection → MySQL**.
2. Set:
   - Server Host: `127.0.0.1`
   - Port: `3306`
   - Database: `auth_db` (optional)
   - Username: `root`
   - Password: `Vishal@90`
3. **Test Connection** (download driver if asked) → **Finish**.

### Port conflict with local Mac MySQL

Only one process can use port `3306`. If an old Oracle/local MySQL is running, stop it before starting the Docker MySQL, or Docker MySQL will fail to bind.

---

## Step-by-step: start the stack (Docker Compose)

From `food-delivery-backend/`:

### 1. Start infrastructure

```bash
cd /Users/vishalsharma/Downloads/Projects/food_dlivery_app/food-delivery-backend

docker compose -p food-delivery-backend up -d mysql-db redis zookeeper kafka
```

Wait until MySQL is ready:

```bash
docker compose -p food-delivery-backend ps
docker exec food_delivery_mysql mysqladmin ping -h127.0.0.1 -uroot -p'Vishal@90'
```

Expect: `mysqld is alive`.

### 2. Start application services

```bash
docker compose -p food-delivery-backend up -d --build \
  auth-service restaurant-service user-service \
  order-service payment-service notification-service \
  api-gateway
```

First builds can take several minutes (Maven + `common-rbac`).

### 3. Verify

```bash
docker compose -p food-delivery-backend ps
curl -s http://localhost:8080/actuator/health
```

Gateway should be **Up** (not Restarting).

### 4. Frontend (Docker container)

With the backend stack already up:

```bash
docker compose -p food-delivery-backend up -d --build frontend
```

Open: **http://localhost:3000**

The UI container serves Next.js. API calls from your **browser** still go to **http://localhost:8080** (api-gateway). That is intentional — do not use `http://api-gateway:8080` in `NEXT_PUBLIC_API_URL` for local Docker.

Or start frontend with everything:

```bash
docker compose -p food-delivery-backend up -d --build
```

Local npm (without Docker frontend):

```bash
cd ../food-delivery-frontend
npm run dev
```

### 5. Stop everything

```bash
cd /Users/vishalsharma/Downloads/Projects/food_dlivery_app/food-delivery-backend
docker compose -p food-delivery-backend down
```

---

## Service ports (Compose)

| Service | URL / address |
|---------|----------------|
| Frontend (Next.js) | `http://localhost:3000` |
| API Gateway (browser API entry) | `http://localhost:8080` |
| Auth | `http://localhost:8081` |
| Restaurant | `http://localhost:8082` |
| Order | `http://localhost:8083` |
| Payment | `http://localhost:8084` |
| Notification | `http://localhost:8085` |
| User | `http://localhost:8086` |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |
| Kafka (from host) | `localhost:9093` |
| Kafka (container-to-container) | `kafka:9092` |

---

## Useful debug commands

```bash
# Status
docker compose -p food-delivery-backend ps

# Logs
docker logs -f food_delivery_api_gateway
docker logs -f food_delivery_mysql
docker logs -f auth_service

# Rebuild one service (e.g. gateway after pom/Dockerfile changes)
docker compose -p food-delivery-backend build --no-cache api-gateway
docker compose -p food-delivery-backend up -d --force-recreate api-gateway
```

### Gateway note

If the gateway crash-loops with Spring Boot **4.x** / `WebMvcAutoConfiguration` errors, the image is stale. Rebuild without cache (see above). The gateway `pom.xml` targets Spring Boot **3.2.4**.

Services that depend on `common-rbac` build with Compose context = backend root so Maven can `install` that module inside the image.

---

## When you move to Kubernetes

1. Confirm Compose stack is fully working.
2. `docker compose -p food-delivery-backend down`
3. Apply K8s manifests in dependency order (MySQL / Redis / Kafka first, then services, then gateway/ingress).
4. Smoke-test the same user flows through the cluster URL.

K8s manifests live under each service’s `k8s/` folder (e.g. `auth-service/k8s/mysql-deployment.yaml`). Treat K8s as a **deploy** path, not the primary local debug path.

---

## Quick mental model

```text
Mac browser / Workbench / frontend
        │
        │  localhost:8080  →  API Gateway container
        │  localhost:3306  →  MySQL container (same DB)
        ▼
Docker network
        │
        │  mysql-db:3306   →  used by auth, order, user, …
        │  kafka:9092      →  used by order, payment, …
        ▼
Other service containers
```

One MySQL container. Two ways to reach it: `localhost` from the Mac, `mysql-db` from other containers.
