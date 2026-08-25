# Configuration Reference

Everything that varies between machines is an environment variable with a
working default. **You can clone this repository and run it without editing a
single file**, provided a MySQL instance is reachable on `127.0.0.1:3306`.
Change anything you need by exporting a variable or writing a `.env`.

## How settings are resolved

Later sources win over earlier ones:

1. Defaults baked into `backend/src/main/resources/application.yml`
2. `.env` in the repository root (optional, gitignored)
3. Real environment variables
4. Command-line arguments (`--server.port=9090`)

`.env` is picked up whether you launch from the repo root or from `backend/`,
via `spring.config.import` — no external dotenv library involved.

Copy the template to get started:

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env.local
```

## Backend

### Database

| Variable | Default | Notes |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | |
| `DB_PORT` | `3306` | |
| `DB_NAME` | `sleekflow_todo` | Created by `./scripts/db.sh bootstrap` |
| `TEST_DB_NAME` | `sleekflow_todo_test` | Used only by the `test` profile |
| `DB_USER` | `todo` | |
| `DB_PASSWORD` | `todo_dev_pw` | Development credential; override in any shared environment |
| `DB_POOL_MAX_SIZE` | `20` | HikariCP maximum pool size |
| `DB_POOL_MIN_IDLE` | `5` | |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | `30000` | |

### HTTP and CORS

| Variable | Default | Notes |
|---|---|---|
| `SERVER_PORT` | `8080` | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Comma-separated. Only matters when the UI is *not* behind the Vite dev proxy |

### Behaviour and diagnostics

| Variable | Default | Notes |
|---|---|---|
| `APP_NAME` | `sleekflow-todo` | |
| `FLYWAY_ENABLED` | `true` | Set `false` if migrations are applied out of band |
| `JPA_DDL_AUTO` | `validate` | Schema is owned by Flyway; `validate` catches drift |
| `JPA_SHOW_SQL` | `false` | |
| `PAGE_SIZE_DEFAULT` | `25` | |
| `PAGE_SIZE_MAX` | `200` | Caps page size so a large list cannot be pulled in one request |
| `API_DOCS_ENABLED` | `true` (`false` under `prod`) | `/v3/api-docs` |
| `SWAGGER_UI_ENABLED` | `true` (`false` under `prod`) | `/swagger-ui.html` |
| `ACTUATOR_ENDPOINTS` | `health,info` | Comma-separated exposure list |
| `LOG_LEVEL_APP` | `DEBUG` (`INFO` under `prod`/`test`) | |
| `LOG_LEVEL_SQL` | `INFO` | Set `DEBUG` to log statements |

### Profiles

| Profile | Activated by | Effect |
|---|---|---|
| _(none)_ | default | Local development defaults |
| `test` | Surefire, automatically | Points at `TEST_DB_NAME`, quieter logging |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | **No credential defaults** — startup fails unless `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` are supplied; API docs off |

The `prod` profile deliberately has no fallback credentials. Failing to start is
better than silently connecting to something unintended.

## Frontend

Vite only exposes variables prefixed with `VITE_`. Put overrides in
`frontend/.env.local` (gitignored).

| Variable | Default | Notes |
|---|---|---|
| `VITE_DEV_PORT` | `5173` | Dev server port |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Where the dev server forwards `/api`. Development only |
| `VITE_API_BASE_URL` | `/api` | Runtime API base. Leave empty for same-origin; set an absolute URL when the API is on another origin |

Because the dev server proxies `/api` to the backend, the default development
setup is same-origin and needs no CORS configuration. CORS only comes into play
if you set `VITE_API_BASE_URL` to an absolute URL — in which case add this app's
origin to `CORS_ALLOWED_ORIGINS`.

## Recipes

**Backend on a different port**

```bash
SERVER_PORT=9090 ./mvnw spring-boot:run
# then, for the frontend:
VITE_API_PROXY_TARGET=http://localhost:9090 npm run dev
```

**Point at an existing MySQL server**

```bash
export DB_HOST=db.internal DB_NAME=todo DB_USER=app DB_PASSWORD=…
cd backend && ./mvnw spring-boot:run
```

**Production-style startup**

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_HOST=… DB_NAME=… DB_USER=… DB_PASSWORD=… \
java -jar backend/target/todo-0.0.1-SNAPSHOT.jar
```

**Different toolchain locations** — `scripts/env.sh` only prepends directories
that exist, so it is a no-op on a machine where `node` and `mysql` are already
on `PATH`. Override with `TOOLS_HOME`, `NODE_HOME`, or `MYSQL_HOME`.

## Local toolchain paths

The development machine this was built on has no Homebrew or Docker, so the
toolchain lives under the user's home directory. None of this is required to
run the project — it is recorded so the setup can be reproduced or removed.

| Path | Contents |
|---|---|
| `~/.local/opt/node` | Node.js 24.19.0 LTS |
| `~/.local/opt/mysql` | MySQL 26.7.0 |
| `~/.local/etc/my.cnf` | MySQL server config |
| `~/.local/var/mysql` | MySQL data directory, socket, error log |

Remove with `rm -rf ~/.local/opt ~/.local/var/mysql ~/.local/etc/my.cnf`.
