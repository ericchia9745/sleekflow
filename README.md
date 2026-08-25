# SleekFlow TODO

A TODO list application with recurring tasks, task dependencies, and filtering/sorting.

- **Backend** — Java 25 · Spring Boot 4.1.1 · Spring Data JPA · Flyway · MySQL
- **Frontend** — React 19 · TypeScript · Vite · TanStack Query
- **API docs** — Swagger UI at http://localhost:8080/swagger-ui.html

## Prerequisites

| Tool | Version used | Notes |
|---|---|---|
| JDK | 25 (Temurin) | `java -version` |
| Maven | via `./mvnw` | no separate install needed |
| Node.js | 24.19.0 LTS | ships npm 11 |
| MySQL | 26.7 | or use `docker compose up -d` |

## Setup

### 1. Database

Either run the bundled Docker service:

```bash
docker compose up -d
```

…or point the app at any MySQL 8+ instance and create the schemas:

```sql
CREATE DATABASE sleekflow_todo      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE sleekflow_todo_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'todo'@'%' IDENTIFIED BY 'todo_dev_pw';
GRANT ALL PRIVILEGES ON sleekflow_todo.*      TO 'todo'@'%';
GRANT ALL PRIVILEGES ON sleekflow_todo_test.* TO 'todo'@'%';
```

Connection settings are read from environment variables with local defaults — see `.env.example`.
Flyway applies the schema migrations in `backend/src/main/resources/db/migration` on startup.

### 2. Backend

```bash
cd backend && ./mvnw spring-boot:run
```

Serves on http://localhost:8080. Health check at `/actuator/health`.

### 3. Frontend

```bash
cd frontend && npm install && npm run dev
```

Serves on http://localhost:5173 and proxies `/api/*` to the backend, so there is no CORS setup in development.

### All at once

```bash
./scripts/dev.sh
```

## Tests

```bash
cd backend && ./mvnw test
```

Integration tests run against the `sleekflow_todo_test` schema under the `test` Spring profile.

```bash
cd frontend && npm test
```

## Repository layout

```
backend/     Spring Boot service (REST API, domain model, migrations)
frontend/    React single-page app
scripts/     Local development helpers (db.sh, dev.sh, env.sh)
docs/        Decision log and architecture notes
docker/      MySQL container init script
```

## Documentation

- [Decision log](docs/DECISION_LOG.md) — requirement interpretations, trade-offs, and scope choices.
