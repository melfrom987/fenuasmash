# ADR 0003 — Competition rules belong to the tournament

- Status: accepted
- Date: 2026-11-16

## Context

The first version of the API stored the format (single elimination, pools…) and the scoring rules on each event. In badminton tournaments, these rules are the same for every event of a tournament, and events are created before registrations, while their pools can only be set once the number of entries is known.

## Decision

- **Format, play mode, scoring rules, disciplines offered and forbidden combinations are tournament rules**, set when the tournament is created and editable until an event starts.
- **An event is a discipline and a category** (open, age or level), with an optional planned date and maximum number of entries. It is created before registrations.
- **The pool structure of an event** (number of pools, pool size, qualifiers) stays empty until the draw, and is set from the actual entries.
- **Courts are numbered** from 1 to the number of courts, with no free names.

See [competition-rules.md](../competition-rules.md) for the full rules.

## Alternatives Considered

| Option | Reason for rejection |
|---|---|
| Rules on each event | Does not match how tournaments are run; the organizer would repeat the same rules for every event |
| Events created after registrations | Players must choose an event when they register, and organizers plan days and limits per event in advance |
| Pool structure required when creating an event | The number of entries is unknown at that time |

## Consequences

- One migration moved format and scoring columns from `event` to `tournament`.
- Registration rules (gender, age category, maximum entries, forbidden combinations) are checked against both the event and the tournament rules.
