# ADR 0004 — Contract-first API with OpenAPI

- Status: proposed
- Date: 2026-11-16

## Context

The backend, the web app and the mobile app are developed in parallel by three people. While the API was only described in pull request descriptions, the web app and the backend implemented diverging versions of the same endpoints (field names, required fields, error formats), and each side waited for the other to know what to build.

## Decision

- **`contracts/openapi.yaml` is the single source of truth** for every endpoint: requests, responses and error codes.
- **The contract is updated before the code**, in a pull request reviewed by the three team members.
- **Operations not implemented yet are marked `x-status: planned`**, so the web and mobile apps can build against them with mocks while the backend implements them.
- **Errors use Problem Details with a machine-readable `code`**, which clients translate; the `detail` text is for developers only.

## Alternatives Considered

| Option | Reason for rejection |
|---|---|
| Contract generated from the backend code (springdoc) | The contract would only exist once the backend is written, which keeps clients waiting |
| Contract described in pull request descriptions | Not versioned in one place, easily out of date, not usable by tools |
| GraphQL | Adds a learning curve for the team; REST with OpenAPI covers the needs |

## Consequences

- The web app can generate its TypeScript types from the contract (e.g. `openapi-typescript`), and the mobile app its Dart client.
- A pull request that changes the API without updating the contract is not merged.
