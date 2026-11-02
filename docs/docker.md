# Docker

Docker runs each component in an isolated container, so the stack behaves identically on every machine. No local installation of PostgreSQL or Java is required.

## Prerequisites

| Tool | Notes |
|---|---|
| Docker Desktop (or Docker Engine with Compose v2) | Must be running |
| `make` | Preinstalled on macOS and Linux. On Windows, use WSL2 |
| Node.js 24 | Only for front-end development (`.nvmrc`) |
| JDK 25 | Optional. Only to run the API outside Docker |

## Quick Start

```bash
make env      # create .env from .env.example (first time only)
make stack    # build and start db, api and web
```

The application is available at http://localhost:8080. The first build takes one to two minutes.

```bash
make down     # stop all containers
```

## Services

| Service | Description | Local address |
|---|---|---|
| `db` | PostgreSQL 17 | `localhost:5432` |
| `api` | Spring Boot API | http://localhost:8081 |
| `api` health | Actuator health endpoint | http://localhost:8081/actuator/health |
| `web` | React application served by nginx | http://localhost:8080 |

## Commands

All commands are run from the repository root.

| Command | Description |
|---|---|
| `make help` | List available commands |
| `make env` | Create `.env` from `.env.example` if missing |
| `make db` | Start the database only |
| `make api` | Build and start the database and the API |
| `make stack` | Build and start the full stack |
| `make ps` | Show container status |
| `make logs` | Follow the logs of all containers |
| `make psql` | Open a SQL shell on the database |
| `make down` | Stop all containers and keep the data |
| `make clean` | Stop all containers and **delete the database volume** |
| `make prod-config` | Print the resolved production configuration |
| `make lint-infra` | Lint workflows (actionlint) and Dockerfiles (hadolint) |

Images are rebuilt from the current source code on every `make api` and `make stack`.

## Development Workflow

### Front end

The Vite dev server provides hot reload and proxies `/api` and `/ws` to the API, mirroring the nginx configuration used in production.

```bash
make api
cd apps/web
npm ci
npm run dev                # http://localhost:5173
```

| Scenario | Command |
|---|---|
| API running in Docker (default) | `npm run dev` |
| API running natively on port 8080 | `API_PROXY_TARGET=http://localhost:8080 npm run dev` |
| No API, mocked data (MSW) | `npm run dev:mock` |

### API

Run `make api` after each change, or run the API natively with a local JDK 25:

```bash
make db
cd apps/api
./mvnw spring-boot:run
```

## Configuration

Local settings are stored in `.env`, which is never committed.

| Variable | Default | Description |
|---|---|---|
| `POSTGRES_USER` | `fenuasmash` | Database user |
| `POSTGRES_PASSWORD` | `change-me` | Database password |
| `POSTGRES_DB` | `fenuasmash` | Database name |
| `POSTGRES_PORT` | `5432` | Host port for the database |
| `API_PORT` | `8081` | Host port for the API |
| `WEB_PORT` | `8080` | Host port for the web application |
| `IMAGE_PREFIX`, `IMAGE_TAG`, `HTTP_PORT` | — | Production only |

## Files

| File | Purpose |
|---|---|
| `docker-compose.yml` | Development stack. `api` and `web` belong to the `stack` profile |
| `docker-compose.prod.yml` | Production stack based on published images |
| `apps/api/Dockerfile` | Multi-stage build: Maven build, JRE 25 runtime, non-root user |
| `apps/web/Dockerfile` | Multi-stage build: Vite build, unprivileged nginx runtime |
| `apps/web/nginx/default.conf.template` | Static file serving, SPA fallback, `/api` and `/ws` proxy |
| `.dockerignore` | Files excluded from the build context |
| `Makefile` | Command shortcuts |

## Troubleshooting

| Symptom | Resolution |
|---|---|
| `Cannot connect to the Docker daemon` | Start Docker Desktop |
| `port is already allocated` | Change the corresponding port in `.env` |
| A container stays `unhealthy` | Inspect `make logs` |
| `502 Bad Gateway` on `/api` | The API is not running. Check `make ps` and `make logs` |
| `password authentication failed` | The volume was created with other credentials. Run `make clean`, then `make db` |
| Low disk space | Run `docker system prune` |
