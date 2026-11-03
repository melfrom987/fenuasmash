# Contributing to FenuaSmash

## Golden Rules

1. **Everything is written in English**: branch names, commit messages, pull request titles and descriptions, issues, review comments, code comments and documentation.
2. **Nobody pushes directly to `dev` or `main`.** Every change goes through an issue, a branch and a pull request approved by at least one other team member.

## 1. Start With an Issue

Every task has an issue in GitHub Projects, with:

- a **scope** label: `web`, `api`, `mobile`, `infra`, `docs`;
- a **type** label: `feature`, `bug`, `chore`.

## 2. Create a Branch

Branches are created from `dev` and named:

```text
type/issue-number-short-description
```

| Type | Use for |
|---|---|
| `feat` | A new feature |
| `fix` | A bug fix |
| `chore` | Configuration, dependencies, tooling |
| `docs` | Documentation only |
| `refactor` | Code restructuring without behavior change |
| `test` | Adding or fixing tests |
| `ci` | CI/CD pipelines |

Examples: `feat/12-court-board`, `fix/27-score-sync`, `chore/3-web-init`.

```bash
git switch dev
git pull
git switch -c feat/12-court-board
```

## 3. Write Commit Messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/):

```text
type(scope): description

Optional body explaining what changed and why.

Refs: #12
```

| Rule | Example |
|---|---|
| Written in English | `add court board page` |
| Imperative mood | `add`, not `added` or `adds` |
| Lowercase after the colon, no trailing period | `fix(api): reject revoked devices` |
| Subject line of 72 characters or fewer | |
| Blank line between subject and body | |

**Types:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `perf`.
**Scopes (optional):** `web`, `api`, `mobile`, `infra`, `contracts`, `docs`, `deps`.

Examples:

```text
feat(web): add court board page
fix(api): reject score events from revoked devices
chore(mobile): upgrade flutter dependencies
docs: add installation guide
```

## 4. Open a Pull Request

- Target branch: **`dev`**.
- The title follows the commit format. It is checked automatically by CI.
- Fill in the pull request template, in English.
- Add `Closes #12` to the description to close the issue on merge.
- At least one approval is required, and the **`CI`** check must be green.
- Pull requests are **squash-merged**: the title becomes the single commit on `dev`.

`dev` is merged into `main` for releases. Every merge into `main` is deployed.

## 5. Keep Your Branch Up to Date

```bash
git fetch
git rebase origin/dev
git push --force-with-lease
```

## API Contract

Any API change starts with `contracts/openapi.yaml`, in a pull request reviewed by both the web and mobile developers. See [contracts/README.md](contracts/README.md).
