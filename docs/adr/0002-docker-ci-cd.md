# ADR 0002 — Containerization and CI/CD

- Status: accepted
- Date: 2026-10-27

## Context

The project specification requires containerized services with Docker (reproducible environments, simpler deployment) and a CI/CD pipeline (automated checks, tests and builds).

## Decision

- **One image per deployable service:** `api` (Spring Boot) and `web` (nginx serving React). The mobile application is distributed as an APK.
- **Multi-stage builds** with non-root runtime users and built-in health checks, to keep images small and secure.
- **nginx reverse-proxies `/api` and `/ws`** to the API, so clients use a single origin: no CORS configuration and a single TLS certificate.
- **Two Compose files:** one for development, one for production, where the database is isolated on an internal network.
- **GitHub Actions CI** runs only the checks relevant to the changed components, and exposes a single aggregated `CI` status check.
- **CD** publishes images to GitHub Container Registry and deploys over SSH with Docker Compose.

## Alternatives Considered

| Option | Reason for rejection |
|---|---|
| Kubernetes | Excessive operational overhead for a single server and a team of three |
| Single image for API and web | Couples release cycles and prevents independent scaling |
| Docker Hub | GitHub Container Registry is integrated with repository permissions |

## Consequences

Applications must follow the conventions listed in the [DevOps overview](../devops.md#conventions).
