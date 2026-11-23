# Development Guide

## Standard workflow

After modifying Dart code:

```bash
dart format .
flutter analyze
```

When tests are available or affected:

```bash
flutter test
```

When ARB files change:

```bash
flutter gen-l10n
dart format .
flutter analyze
```

## Adding a feature

Prefer placing feature-specific code inside its feature rather than adding it to `core`.

Typical structure:

```text
features/example/
├── application/
├── data/
├── domain/
└── presentation/
```

Not every feature needs every folder. Create a layer only when it has a real responsibility.

## Widget extraction

Do not split files solely because they are long. Extract a widget when it represents a meaningful visual concept or has a clear input/output API.

Good examples:

- `CourtSetupTeamCard`;
- `CourtSetupPlayerRow`;
- `CountdownBanner`;
- `BadmintonCourt`.

A useful extracted widget should receive its dependencies through constructor parameters instead of reaching into a parent `State` object.

## Match logic changes

When changing scoring or positioning behavior:

1. Update `BadmintonRules` if the change is a configurable rule/constant.
2. Implement the transition in `MatchController` or the appropriate controller part.
3. Avoid duplicating the rule in a presentation widget.
4. Add or update unit tests.
5. Verify undo and draft restoration if the changed state is persisted.

## Persistence changes

When adding a field required to resume a match:

1. Add it to the appropriate domain model.
2. Serialize it in `toJson()`.
3. Restore it in `fromJson()`.
4. Decide how older saved data should be migrated or defaulted.
5. Test a serialize/deserialize round trip.

If the field is merely a global user preference, store it in settings instead of the match draft.

## Responsive UI

Use `Responsive.of(context)` rather than scattering device-specific numeric values through widgets.

The active match and court setup are landscape-oriented and should prioritize fitting essential umpire controls without scrolling.

Test at least:

- phone landscape;
- larger phone landscape;
- tablet landscape;
- supported text scaling range.

## Localization

All stable user-facing strings should come from `AppLocalizations`. During rapid prototyping, temporary hard-coded strings may be acceptable, but they should be localized before the page is considered finished.

## Naming and identity

Do not introduce new logic that assumes display names are globally unique. Before backend integration, migrate technical player references to stable IDs.

## Recommended test strategy

### Unit tests

Prioritize `MatchController` and serialization because they contain deterministic behavior and tournament-critical state.

### Widget tests

Use widget tests for important UI state transitions such as disabled/enabled setup buttons and interval overlays.

### Integration tests

Later, cover a complete match flow from assigned/configured match through final history entry.

## Before backend integration

Recommended checklist:

- stable player IDs instead of names as identifiers;
- `CourtSetup` used as a real model;
- controller rule tests in place;
- draft round-trip tests in place;
- clear API DTO boundary;
- local recovery behavior retained;
- no direct PostgreSQL access from Flutter.

## Before a commit

```bash
dart format .
flutter analyze
flutter test
```

Also check that generated localization files are synchronized if ARB files changed.
