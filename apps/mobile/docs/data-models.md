# Data Models

## `MatchConfig`

`MatchConfig` contains fixed administrative information about a match.

Current fields include:

- singles/doubles flag;
- Team A and Team B player names;
- tournament name;
- court number;
- category;
- optional age category;
- optional pool/group;
- optional tournament phase;
- umpire name;
- optional service judge name.

It supports JSON serialization and is embedded inside `MatchDraft`.

### Responsibility

`MatchConfig` should answer **"What match is being played?"** It should not contain live score, timers or mutable court state.

## `CourtSetup`

`CourtSetup` represents decisions made immediately before the match on the physical court.

Its current model contains:

- `leftSideTeamId`;
- `serverName`;
- `receiverName`.

The model is currently not yet the primary object passed into `MatchPage`. It should be adopted so related court-setup parameters are no longer passed independently.

A planned extension is to include initial player colors. For long-term domain independence, colors should preferably be serialized as integer color values rather than Flutter `Color` objects.

## `TeamCourtState`

`TeamCourtState` represents one match side's physical player state. In doubles it stores which player is currently shown in the top and bottom positions.

The active controller owns one state for Team A and one for Team B.

## `MatchPhase`

`MatchPhase` defines the current match state:

```text
playing
midSetIntervalReady
midSetIntervalRunning
betweenSetsIntervalReady
betweenSetsIntervalRunning
preparingNextSet
finished
```

Using an enum prevents the UI from inferring match state from unrelated booleans.

## `MatchSnapshot`

A snapshot captures enough mutable match state to support undo. The controller keeps a private snapshot history and exposes it as an unmodifiable list.

Snapshots are preferable to manually reversing a rally because a single rally can modify several pieces of state.

## `MatchEvent`

`MatchEvent` stores events associated with the active match. Events are serialized into drafts so they survive recovery.

## `MatchDraft`

`MatchDraft` is the complete recoverable representation of an active match.

It includes:

- stable `matchId`;
- `MatchConfig`;
- scores and games won;
- server/receiver state;
- left-side assignment;
- Team A / Team B court state;
- interval state;
- previous games;
- current `MatchPhase`;
- undo history;
- match events;
- match timing;
- warm-up timing;
- shuttle count;
- player colors;
- save timestamp.

### Serialization version

The current JSON format writes `version: 2`. Version 2 nests fixed match information under `config`. `MatchDraft.fromJson` still accepts the previous structure where those fields were stored directly in the draft.

Compatibility getters such as `draft.tournamentName` currently forward to `draft.config`. They are transitional helpers and can eventually be removed after all consumers use `draft.config` directly.

## `MatchHistoryEntry`

A history entry represents a completed match for the history feature. Unlike `MatchDraft`, it is intended for long-term result consultation rather than resuming live scoring.

## Identity warning

The current application often uses `playerName` as a technical identifier, including server/receiver selection and the player-color map.

This is acceptable for the prototype but should change before backend integration because two players can share the same display name.

Recommended future model:

```dart
class Player {
  final String id;
  final String displayName;
}
```

Then references become `serverPlayerId`, `receiverPlayerId` and maps keyed by player ID.

## Team naming warning

`teamA` and `teamB` are useful stable internal sides, but they do not necessarily represent clubs or formal teams. In singles, each side contains one player. Future backend models may therefore use concepts such as `MatchSide` or `Entry` while preserving Team A / Team B as internal match-side identifiers.
