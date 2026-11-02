# DevOps Overview

This document describes how FenuaSmash is built, run and delivered. It is the entry point to the operational documentation.

| Document | Scope |
|---|---|
| [Docker](docker.md) | Running the stack locally, commands, troubleshooting |
| [CI/CD](ci-cd.md) | Automated checks on pull requests, image publishing, releases |
| [Installation](installation.md) | Production server setup and deployment |
| [ADR 0002](adr/0002-docker-ci-cd.md) | Rationale behind the technical choices |

## Architecture

```text
 Browser ──► web (nginx + React) ──► api (Spring Boot) ──► db (PostgreSQL)
                                        ▲
 Tablet (Flutter app) ──────────────────┘
```

| Component | Technology | Delivery |
|---|---|---|
| `db` | PostgreSQL 17 | Docker image |
| `api` | Spring Boot 4, Java 25, Maven | Docker image |
| `web` | React, TypeScript, Vite, served by nginx | Docker image |
| `mobile` | Flutter | Android APK |

nginx serves the web application and reverse-proxies `/api` and `/ws` to the API. Clients therefore use a single origin, which removes the need for CORS configuration.

## Delivery Workflow

| Step | Action | Outcome |
|---|---|---|
| 1 | Develop and test locally with Docker | `make stack` runs the full system |
| 2 | Push a branch and open a pull request to `dev` | CI runs automatically |
| 3 | CI passes and the pull request is approved | Changes are squash-merged into `dev` |
| 4 | `dev` is merged into `main` | CD publishes images and deploys |
| 5 | A `vX.Y.Z` tag is pushed | A GitHub release is created with the APK |

## Conventions

The build and CI pipelines rely on the following conventions.

**API (`apps/api`)**
- Maven project with the Maven Wrapper (`mvnw`, `.mvn/`) committed.
- `spring-boot-starter-actuator` is required: `/actuator/health` is the container health check.
- Database settings are provided through `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. Never hard-code them.
- REST endpoints are served under `/api`, the WebSocket endpoint under `/ws`.

**Web (`apps/web`)**
- `package-lock.json` is committed.
- The API is called with relative paths (`/api/...`), never with an absolute URL.

**All applications**
- Any new environment variable is added to `.env.example`.
- Branches, commits and pull requests follow [CONTRIBUTING.md](../CONTRIBUTING.md).
