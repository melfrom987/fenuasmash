# FenuaSmash API

Spring Boot 4 backend for FenuaSmash: REST API, data model and real-time score broadcasting.

| | |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator) |
| Database | PostgreSQL 17, schema managed by Flyway |
| Build | Maven Wrapper (`./mvnw`) |
| Tests | JUnit 5, MockMvc, Testcontainers |

## Getting Started

Prerequisites: Docker Desktop (running). A local JDK 25 is only needed to run the API outside Docker.

| Task | Command |
|---|---|
| Run the API in Docker | `make api` (from the repository root) → http://localhost:8081 |
| Run the API natively | `make db`, then `./mvnw spring-boot:run` in `apps/api` → http://localhost:8080 |
| Run all tests | `./mvnw verify` in `apps/api` |
| Health check | `GET /actuator/health` |

Tests start their own PostgreSQL container with Testcontainers: no running database is required, only Docker.

## Project Structure

Code is organized **by feature**, not by technical layer. Each feature is self-contained in its own package.

```text
src/main/java/com/tao/fenuasmash/
├── FenuasmashApplication.java
├── common/
│   └── error/                         Shared error handling
│       ├── GlobalExceptionHandler.java
│       └── NotFoundException.java
└── organization/                      Reference feature: copy it for new features
    ├── Organization.java              JPA entity
    ├── OrganizationRepository.java    Spring Data repository (package-private)
    ├── OrganizationService.java       Business logic and transactions
    ├── OrganizationController.java    HTTP endpoints only
    ├── OrganizationResponse.java      Output DTO
    └── CreateOrganizationRequest.java Input DTO, validated

src/main/resources/
├── application.yaml
└── db/migration/                      Flyway migrations (V1__..., V2__...)

src/test/java/com/tao/fenuasmash/
├── TestcontainersConfiguration.java   PostgreSQL container shared by all tests
└── organization/
    └── OrganizationControllerTests.java
```

## Adding a Feature

Use the `organization` package as the template. For a new feature (for example, `player`):

1. **Migration.** Add `src/main/resources/db/migration/V<next>__create_player.sql`.
2. **Entity.** Create `player/Player.java`: UUID identifier generated in the constructor, `Instant` timestamps truncated to microseconds, a `protected` no-argument constructor for JPA, no public setters.
3. **Repository.** Create `player/PlayerRepository.java`, package-private, extending `JpaRepository<Player, UUID>`.
4. **DTOs.** Create `PlayerResponse` (with a static `from(Player)` method) and `CreatePlayerRequest` with validation annotations. Use Java `record`s.
5. **Service.** Create `PlayerService`, annotated `@Transactional(readOnly = true)`, with `@Transactional` on write methods. Throw `NotFoundException` for missing resources.
6. **Controller.** Create `PlayerController` under `/api/players`. It only maps HTTP to service calls: no business logic.
7. **Tests.** Create `PlayerControllerTests`, covering success cases, validation errors and not-found cases.
8. Run `./mvnw verify`, then `make api` and check the endpoints manually.

## Conventions

### Architecture

- Requests flow **Controller → Service → Repository**, never the other way around.
- **Entities are never returned by the API.** Controllers return DTOs only.
- Other features access a feature through its **service**, never through its repository.
- Dependencies are injected **through constructors**. Never use `@Autowired` on fields.

### Data

- The database schema is changed **only by Flyway migrations**. Hibernate runs with `ddl-auto: validate`.
- **Never edit a migration that has been merged.** Add a new one instead.
- Identifiers are **UUIDs**. Dates are **`Instant`**, stored and returned in **UTC** (ISO 8601).
- Table and column names use `snake_case`.

### API

- Every endpoint is under `/api`. The WebSocket endpoint is `/ws`.
- Request bodies are validated with `@Valid` and Jakarta Validation annotations.
- Creation endpoints return **201 Created** with a `Location` header.
- Every error is returned as an RFC 9457 **Problem Details** response (`application/problem+json`), with `title`, `status`, `detail` and `instance`. Unexpected errors return a generic message and are logged server-side.
- The API contract is defined in [`contracts/`](../../contracts/README.md). Update it before changing an endpoint.

### Configuration

- Never hard-code credentials. Database settings come from `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`, provided by Docker and CI.
- Only `/actuator/health` is exposed.

### Code Style

- Everything is written in English: code, comments, commit messages.
- Use SLF4J for logging. Never use `System.out.println`.
- Test classes are named `*Tests` so that Maven runs them during `./mvnw verify`.

## Further Reading

- [Docker guide](../../docs/docker.md)
- [CI/CD guide](../../docs/ci-cd.md)
- [Contributing guidelines](../../CONTRIBUTING.md)
