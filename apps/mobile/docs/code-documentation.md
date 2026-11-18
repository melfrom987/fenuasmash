# Code Documentation Conventions

## Do we need comments in the code?

Yes, but selectively. Comments should explain **intent, rules, invariants or non-obvious decisions**. They should not translate obvious Dart statements into prose.

Documentation files explain the system as a whole. DartDoc comments explain the contract and purpose of important code elements close to their implementation. Both are useful and serve different purposes.

## Prefer DartDoc for important public concepts

Use `///` on important classes, models, controllers, services and public methods whose behavior is not obvious.

Example:

```dart
/// Controls the mutable state and badminton rules of an active match.
///
/// The controller owns scoring, service/receiver transitions, court positions,
/// intervals, match phases, events and undo snapshots. UI-specific behavior
/// such as dialogs and navigation must remain outside this class.
class MatchController extends ChangeNotifier {
  // ...
}
```

For `MatchConfig`:

```dart
/// Fixed administrative configuration of a badminton match.
///
/// This data describes the match itself and does not contain mutable scoring
/// state or on-court decisions made after the toss.
class MatchConfig {
  // ...
}
```

For `CourtSetup`:

```dart
/// On-court decisions made by the umpire before warm-up starts.
///
/// This is intentionally separate from [MatchConfig], which contains the
/// administrative information supplied when the match is created or assigned.
class CourtSetup {
  // ...
}
```

## Comment badminton-specific decisions

A reader may understand Dart but not immediately understand why a rule is implemented in a particular order.

Useful example:

```dart
// In the deciding game, teams change ends at the mid-game interval.
// Score and service state are preserved.
if (previousSets.length == 2) {
  _swapEnds();
}
```

Another useful example:

```dart
// Store scores in canonical Team A / Team B order. The UI maps them to
// physical left/right sides when displaying previous games.
previousSets.add([finishedTeamAScore, finishedTeamBScore]);
```

## Comment persistence migrations

Migration comments are valuable because they explain why apparently redundant compatibility code exists.

The existing `MatchDraft` version-1/version-2 migration comments are a good use of comments and should be kept until old data support is intentionally removed.

## Avoid obvious comments

Avoid:

```dart
// Increment Team A score.
teamAScore++;
```

Avoid:

```dart
// Return the right team ID.
return leftSideTeamId == 'teamA' ? 'teamB' : 'teamA';
```

The code already communicates those facts.

## Avoid large decorative section comments when extraction is clearer

Section markers can help inside one large controller, but if a widget contains many unrelated 100-line sections, prefer extracting meaningful widgets or classes rather than adding more headings.

## Document invariants

Comments are especially useful for constraints that must always remain true, for example:

- server and receiver must belong to opposite match sides;
- `previousSets` is always Team A / Team B ordered;
- interval deadlines, not timer ticks, are the source of truth;
- settings are global and must not be persisted in `MatchDraft`;
- player display names are currently identifiers only as a prototype limitation.

## TODO comments

Use TODOs only for concrete future work and make them searchable.

Good:

```dart
// TODO: Replace player-name identifiers with backend player IDs.
```

Poor:

```dart
// TODO: improve this.
```

## Language

Because class names, APIs and repository documentation are in English, use English for new technical DartDoc comments. User-facing text remains localized through ARB files.

Existing French comments do not need to be rewritten immediately. Consistency can be improved gradually when files are touched.

## Recommended first documentation targets

Add DartDoc first to:

1. `MatchController`;
2. `BadmintonRules`;
3. `MatchConfig`;
4. `CourtSetup`;
5. `MatchDraft`;
6. `MatchDraftService`;
7. `MatchHistoryService`;
8. `AppSettingsController`.

Do not attempt to comment every private `_build...()` method or every widget property. The goal is maintainability, not comment volume.
