# ADR 0001 — Single Repository for All Applications

- Status: accepted
- Date: 2026-10-27

## Context

The system consists of three applications (web, API, mobile) that share an API contract and must remain consistent with each other.

## Decision

Use a monorepo: `apps/web`, `apps/api`, `apps/mobile`, and a `contracts/` directory for the OpenAPI contract.

## Consequences

- A contract change and the related code changes can be delivered in the same pull request.
- A single Docker and CI configuration covers the whole system.
- CI filters by directory to run only the relevant checks.
