# CI/CD

The pipelines run on GitHub Actions and require no manual action.

- **Continuous Integration (CI)** validates every pull request: build, tests, Docker images and a full-stack smoke test.
- **Continuous Delivery (CD)** publishes container images and deploys when changes reach `main`.

## Continuous Integration

**Workflow:** `.github/workflows/ci.yml`
**Triggers:** pull requests, pushes to `dev` and `main`, manual dispatch.

Only the components affected by a change are checked. Components that are not yet initialized are skipped.

| Job | Runs when | Checks |
|---|---|---|
| API | `apps/api` or `contracts` changes | `./mvnw verify` against a PostgreSQL service |
| Web | `apps/web` or `contracts` changes | `lint`, `format:check`, `typecheck`, `test`, `build` |
| Mobile | `apps/mobile` changes | `dart format`, `flutter analyze`, `flutter test`, debug APK build |
| OpenAPI contract | `contracts/openapi.yaml` changes | Redocly lint |
| Infra | Workflows, Dockerfiles or compose files change | actionlint, hadolint, compose validation |
| Docker images | An application or infra changes | Builds the `api` and `web` images |
| Smoke test | API and web both exist | Starts the stack with Docker Compose and probes the health endpoints |
| **CI** | Always | Aggregates all results. **Required status check for merging** |

A skipped job is expected when the related component has not changed.

### Pull Request Title

**Workflow:** `.github/workflows/pr-title.yml`

Pull requests are squash-merged, so the title becomes the commit message. It must follow [Conventional Commits](https://www.conventionalcommits.org/):

```text
type(scope): description
```

| Allowed types | Allowed scopes |
|---|---|
| `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `perf` | `web`, `api`, `mobile`, `infra`, `contracts`, `docs`, `deps` |

### Resolving a Failure

Open **Details** next to the failing check to read the error, then reproduce it locally:

| Failing check | Local command |
|---|---|
| API | `cd apps/api && ./mvnw verify` |
| Web | `cd apps/web && npm run lint && npm run format:check && npm run typecheck && npm run build` |
| Docker images / Smoke test | `make stack` |
| Infra | `make lint-infra` |
| PR title | Edit the pull request title |

Test reports and coverage are available in the **Artifacts** section of the workflow run.

## Continuous Delivery

**Workflow:** `.github/workflows/cd.yml`

| Trigger | Result |
|---|---|
| Push to `main` | `api` and `web` images published to GitHub Container Registry, tagged `latest` and `sha-<commit>`, then deployed if the `production` environment is configured |
| Tag `vX.Y.Z` | Images tagged `X.Y.Z` and `X.Y`, and a GitHub release created with the Android APK |

### Creating a Release

```bash
git switch main
git pull
git tag v1.0.0
git push origin v1.0.0
```

### Rolling Back

Every deployment uses an immutable `sha-<commit>` tag. To roll back, redeploy a previous tag on the server:

```bash
IMAGE_TAG=sha-<commit> docker compose -f docker-compose.prod.yml --env-file .env up -d
```

Server and repository configuration is described in [installation.md](installation.md).

## Dependency Updates

**Configuration:** `.github/dependabot.yml`

Dependabot opens weekly pull requests against `dev` for Maven, npm, pub, Docker and GitHub Actions dependencies. They go through the same CI checks as any other pull request.
