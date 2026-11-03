# FenuaSmash

Badminton competition management: tournament administration, tablet-based umpiring and real-time score broadcasting. Epitech project 2026–2027, designed for French Polynesia.

## Repository Structure

| Directory | Content | Technology |
|---|---|---|
| `apps/web` | Administration and public display | React, TypeScript |
| `apps/api` | API, data model, real time | Spring Boot 4, Java 25, Maven |
| `apps/mobile` | Umpire application | Flutter |
| `contracts` | Shared API contract (OpenAPI) | — |
| `docs` | Technical, installation and user documentation | — |

## Quick Start

Requires Docker Desktop (running) and `make`.

```bash
git clone git@github.com:melfrom987/fenuasmash.git && cd fenuasmash
make env      # create .env (first time only)
make stack    # start database, API and web application
```

Open http://localhost:8090. Stop everything with `make down`.

## Documentation

| Document | Content |
|---|---|
| [DevOps Overview](docs/devops.md) | Architecture, delivery workflow, conventions |
| [Docker](docs/docker.md) | Running the stack, commands, troubleshooting |
| [CI/CD](docs/ci-cd.md) | Pull request checks, releases, deployment |
| [Installation](docs/installation.md) | Production server setup |
| [Architecture Decisions](docs/adr/) | Rationale behind technical choices |

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before creating your first branch. Everything pushed to GitHub (branches, commits, pull requests, issues) is written in English.

## Team

| Member | Role |
|---|---|
| Mélissa Mi You | Web interface and administration |
| Raphael Vaz | TBD |
| Emeric Somnard | TBD |
