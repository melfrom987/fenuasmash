# API Contract

`openapi.yaml` (to be written) describes every API endpoint: requests, responses and errors. It is the **single source of truth** shared by the backend, the web application and the mobile application.

- The **web** application generates its TypeScript types from this file.
- The **mobile** application generates its Dart client from this file.
- The **backend** implements exactly what is described.

## Rules

1. The contract is updated **before** the code.
2. Every pull request touching `contracts/` is reviewed by all three team members.
3. Dates are always UTC, in ISO 8601 format. They are displayed in Tahiti time (`Pacific/Tahiti`).
4. Identifiers are UUIDs.
