# SleekFlow TODO

A TODO list application with recurring tasks, task dependencies, and filtering/sorting.

## Features

- **CRUD** with validation and RFC 9457 problem responses.
- **Recurring TODOs** — daily, weekly, monthly, or every N days. Completing one
  schedules the next occurrence automatically and links it to the series.
- **Dependencies** — a TODO can depend on others and cannot move to *In progress*
  until they are all completed. Cycles are rejected when the edge is added.
- **Filtering** by status, priority, due-date range, blocked/unblocked, and name;
  **sorting** by due date, priority, status, or name — priority and status in
  their natural order, not alphabetically.
- **Soft delete** — deleting is reversible; deleted TODOs stay in a recycle-bin view.
- **Concurrent-safe writes** — optimistic locking rejects a stale write with 409
  rather than silently overwriting another user's edit.

- **Backend** — Java 25 · Spring Boot 4.1.1 · Spring Data JPA · Flyway · MySQL
- **Frontend** — React 19 · TypeScript · Vite · TanStack Query
- **API docs** — Swagger UI at http://localhost:8080/swagger-ui.html

## Quick start

There is **nothing to edit before running.** Every setting has a working default
that targets a local MySQL on `127.0.0.1:3306`; override any of them through the
environment. See **[docs/CONFIGURATION.md](docs/CONFIGURATION.md)** for the
complete reference.

```bash
# 1. A database (either option works)
docker compose up -d                 # containerised MySQL, or…
./scripts/db.sh start                # a locally installed MySQL, then:
./scripts/db.sh bootstrap            # create the schemas and app user

# 2. Backend  -> http://localhost:8080
cd backend && ./mvnw spring-boot:run

# 3. Frontend -> http://localhost:5173
cd frontend && npm install && npm run dev
```

Or start the backend and frontend together with `./scripts/dev.sh`.

## Prerequisites

| Tool | Version used | Notes |
|---|---|---|
| JDK | 25 | Any JDK 17+ works; `java.version` in the POM is 25 |
| Maven | — | Supplied by `./mvnw`; no install needed |
| Node.js | 24.19.0 LTS | Ships npm 11 |
| MySQL | 26.7 | 8.0+ is fine; or use `docker compose up -d` |

## Configuration

Configuration is environment-driven, in this order of precedence (later wins):

1. Defaults in `backend/src/main/resources/application.yml`
2. `.env` in the repository root — optional, gitignored, read whether you launch
   from the repo root or from `backend/`
3. Real environment variables
4. Command-line arguments (`--server.port=9090`)

```bash
cp .env.example .env                        # backend settings
cp frontend/.env.example frontend/.env.local  # frontend settings
```

The settings you are most likely to need:

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `127.0.0.1` / `3306` / `sleekflow_todo` | Database location |
| `DB_USER` / `DB_PASSWORD` | `todo` / `todo_dev_pw` | Database credentials |
| `SERVER_PORT` | `8080` | Backend port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,…` | Only needed if the UI is not behind the dev proxy |
| `VITE_DEV_PORT` | `5173` | Frontend port |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Where the dev server forwards `/api` |

Three profiles exist: the default (local development), `test` (activated
automatically by Surefire, uses a separate schema), and `prod`. The `prod`
profile ships **no credential defaults** — it refuses to start and names the
missing variables rather than falling back to development values.

## Tests

```bash
cd backend  && ./mvnw test    # 65 tests: domain units, service integration, web layer
cd frontend && npm test       # 12 tests: error mapping and table rendering
```

Backend integration tests run against a real MySQL schema (`sleekflow_todo_test`)
rather than an in-memory substitute, because several rules — the blocked filter,
the rank-based sorts, the soft-delete predicate — are enforced in SQL and would
pass against a mock while failing in production. They use their own schema, so a
test run never touches development data.

## Repository layout

```
backend/     Spring Boot service (REST API, domain model, Flyway migrations)
frontend/    React single-page app
scripts/     Local development helpers (db.sh, dev.sh, env.sh)
docs/        Configuration reference and decision log
docker/      MySQL container init hook
```

## Documentation

- **[Decision log](docs/DECISION_LOG.md)** — how ambiguous requirements were
  interpreted, architectural trade-offs, what was deliberately left out.
- **[Configuration reference](docs/CONFIGURATION.md)** — every setting, its
  default, and recipes for common changes.
- **[API reference](docs/openapi.json)** — exported OpenAPI 3 document. The same
  spec is served live at `/v3/api-docs`, with Swagger UI at `/swagger-ui.html`.
