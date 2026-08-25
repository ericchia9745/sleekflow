# SleekFlow TODO

A TODO list application with recurring tasks, task dependencies, and filtering/sorting.

## Features

- **CRUD** with validation and RFC 9457 problem responses.
- **Recurring TODOs** — daily, weekly, monthly, or every N days. Completing one
  schedules the next occurrence automatically and links it to the series.
- **Dependencies** — a TODO can depend on others and cannot move to *In progress*
  until they are all completed. Cycles are rejected when the edge is added. The
  list names each prerequisite in a *Depends on* column — outstanding ones
  highlighted, settled ones struck through — so a blocked task says what it is
  waiting for without opening the row.
- **Filtering** by status, priority, due-date range, blocked/unblocked, and name;
  **sorting** by due date, priority, status, or name — priority and status in
  their natural order, not alphabetically.
- **Soft delete** — deleting is reversible; deleted TODOs stay in a recycle-bin view.
- **Concurrent-safe writes** — optimistic locking rejects a stale write with 409
  rather than silently overwriting another user's edit.
- **Accounts and sessions** — registration and sign-in, with sessions stored in
  the database and addressed by an opaque bearer token. Passwords are hashed in
  the browser and again, salted, on the server.
- **Live updates** — a tab picks up another user's changes within five seconds
  by polling a cheap revision endpoint, and another tab's changes instantly over
  a BroadcastChannel.

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

Optionally load demo data — a dependency chain, recurring chores, an overdue
task, plus 12,000 generated rows to exercise paging and filtering at scale:

```bash
./scripts/seed.sh          # ./scripts/seed.sh 0 for just the demo set
```

Or start the backend and frontend together with `./scripts/dev.sh`.

The API requires a session: create an account on first load. `crypto.subtle`
hashes the password in the browser, which needs a secure context — `localhost`
qualifies, any other host needs HTTPS.

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
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `127.0.0.1` / `3306` / `sleekflow_schedule_note` | Database location |
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
cd backend  && ./mvnw test    # 99 tests: domain, service, security, web layer
cd frontend && npm test       # 24 tests: hashing, session storage, cross-tab channel, table
```

Backend integration tests run against a real MySQL schema (`sleekflow_schedule_note_test`)
rather than an in-memory substitute, because several rules — the blocked filter,
the rank-based sorts, the soft-delete predicate — are enforced in SQL and would
pass against a mock while failing in production. They use their own schema, so a
test run never touches development data.

## Repository layout

```
backend/     Spring Boot service (REST API, domain model, Flyway migrations)
frontend/    React single-page app
scripts/     Local development helpers (db.sh, dev.sh, env.sh, seed.sh)
docs/        Configuration reference and decision log
docker/      MySQL container init hook
```

## Documentation

- **[Architecture](docs/ARCHITECTURE.md)** — how authentication, sessions, and
  the live-update mechanism fit together, with diagrams and their trade-offs.
- **[Decision log](docs/DECISION_LOG.md)** — how ambiguous requirements were
  interpreted, architectural trade-offs, what was deliberately left out.
- **[Configuration reference](docs/CONFIGURATION.md)** — every setting, its
  default, and recipes for common changes.
- **[API reference](docs/openapi.json)** — exported OpenAPI 3 document. The same
  spec is served live at `/v3/api-docs`, with Swagger UI at `/swagger-ui.html`.
