import { delay, http, HttpResponse } from 'msw';
import type { Club, Gender, Player, Registration, RegistrationStatus } from '@/api/models';
import type {
  ImportEntryReport,
  ImportIssue,
  ImportReport,
  ImportRow,
  ImportRowReport,
  MatchReason,
  PlayerCandidate,
  PlayerMatch,
} from '@/features/registration-import/types';
import { requiredGender } from '@/features/registrations/constants';
import { db } from '../data';
import { toPlayerSummary } from '../registrations';
import { normalize, problem } from './utils';

const issue = (code: string, message: string): ImportIssue => ({ code, message });

function levenshtein(a: string, b: string) {
  const row = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    let previous = row[0];
    row[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const current = row[j];
      row[j] = Math.min(row[j] + 1, row[j - 1] + 1, previous + (a[i - 1] === b[j - 1] ? 0 : 1));
      previous = current;
    }
  }
  return row[b.length];
}

const nameKeys = (first: string, last: string) => [
  normalize(`${first} ${last}`),
  normalize(`${last} ${first}`),
];

function candidatesFor(name: string, birthDate?: string, clubName?: string): PlayerCandidate[] {
  const wanted = normalize(name);
  return db.players
    .flatMap((player): PlayerCandidate[] => {
      const keys = nameKeys(player.firstName, player.lastName);
      const sameName = keys.includes(wanted);
      const similar = !sameName && Math.min(...keys.map((k) => levenshtein(k, wanted))) <= 2;
      if (!sameName && !similar) return [];
      const reasons: MatchReason[] = [sameName ? 'SAME_NAME' : 'SIMILAR_NAME'];
      if (birthDate) {
        reasons.push(player.birthDate === birthDate ? 'SAME_BIRTH_DATE' : 'DIFFERENT_BIRTH_DATE');
      }
      if (clubName && normalize(player.club.name) === normalize(clubName)) {
        reasons.push('SAME_CLUB');
      }
      const score =
        (sameName ? 0.6 : 0.4) +
        (reasons.includes('SAME_BIRTH_DATE') ? 0.3 : 0) +
        (reasons.includes('SAME_CLUB') ? 0.1 : 0);
      return [{ player: toPlayerSummary(player), score, reasons }];
    })
    .sort((a, b) => b.score - a.score)
    .slice(0, 5);
}

function classify(candidates: PlayerCandidate[], withBirthDate: boolean): PlayerMatch {
  const has = (c: PlayerCandidate, reason: MatchReason) => c.reasons.includes(reason);
  const exact = candidates.filter(
    (c) => has(c, 'SAME_NAME') && (!withBirthDate || has(c, 'SAME_BIRTH_DATE')),
  );
  const probable = candidates.filter(
    (c) => has(c, 'SIMILAR_NAME') && (!withBirthDate || has(c, 'SAME_BIRTH_DATE')),
  );
  const pick = (result: PlayerMatch['result'], id: string | null): PlayerMatch => ({
    result,
    selectedPlayerId: id,
    candidates,
  });
  if (exact.length === 1 && candidates.length === 1) return pick('EXACT', exact[0].player.id);
  if (exact.length === 1 && withBirthDate) return pick('EXACT', exact[0].player.id);
  if (exact.length === 0 && probable.length === 1 && candidates.length === 1) {
    return pick('PROBABLE', probable[0].player.id);
  }
  return candidates.length > 0 ? pick('AMBIGUOUS', null) : pick('NONE', null);
}

type RowState = {
  row: ImportRow;
  match: PlayerMatch;
  playerId: string | null;
  blocked: boolean;
  issues: ImportIssue[];
};

type EntryState = {
  report: ImportEntryReport;
  coveredByRow: number | null;
  partnerRow: number | null;
  partnerId: string | null;
};

function analyse(tournamentId: string, rows: ImportRow[]) {
  const events = db.events.filter((e) => e.tournamentId === tournamentId);
  const findEvent = (id: string) => events.find((e) => e.id === id);

  const states = new Map<number, RowState>();
  for (const row of rows) {
    let match = classify(
      candidatesFor(`${row.firstName} ${row.lastName}`, row.birthDate, row.clubName),
      true,
    );
    const decision = row.playerDecision;
    if (decision?.action === 'USE_EXISTING')
      match = { ...match, selectedPlayerId: decision.playerId };
    if (decision?.action === 'CREATE_NEW') match = { ...match, selectedPlayerId: null };

    const issues: ImportIssue[] = [];
    if (match.result === 'AMBIGUOUS' && !decision) {
      issues.push(issue('PLAYER_UNRESOLVED', 'Several possible profiles: choose one'));
    }
    const clubExists = db.clubs.some((c) => normalize(c.name) === normalize(row.clubName));
    if (!clubExists && !row.clubCity?.trim()) {
      issues.push(issue('CLUB_CITY_REQUIRED', 'New club without city'));
    }
    states.set(row.rowNumber, {
      row,
      match,
      playerId: match.selectedPlayerId,
      blocked: issues.length > 0,
      issues,
    });
  }

  const rowNamed = (name: string, except: number) => {
    const wanted = normalize(name);
    const found = rows.filter(
      (r) => r.rowNumber !== except && nameKeys(r.firstName, r.lastName).includes(wanted),
    );
    return found.length === 1 ? found[0] : undefined;
  };

  const planned = new Map<string, number>();
  const entries = new Map<number, EntryState[]>();

  for (const state of states.values()) {
    const { row } = state;
    const rowEntries: EntryState[] = [];
    const acceptedDisciplines: string[] = [];

    for (const entry of row.entries) {
      const event = findEvent(entry.eventId);
      const issues: ImportIssue[] = [];
      let status: RegistrationStatus | null = null;
      let partnerMatch: PlayerMatch | null = null;
      let partnerRow: number | null = null;
      let partnerId: string | null = null;
      let partnerGender: Gender | null = null;
      let coveredByRow: number | null = null;

      if (!event) {
        issues.push(issue('EVENT_NOT_FOUND', 'Unknown event'));
      } else if (!state.blocked) {
        if (entry.partner.kind === 'NONE') status = 'CONFIRMED';
        else if (entry.partner.kind === 'LOOKING') status = 'LOOKING_FOR_PARTNER';
        else {
          const name = entry.partner.name;
          const decision = entry.partnerDecision;
          const other = decision ? undefined : rowNamed(name, row.rowNumber);
          if (other && !states.get(other.rowNumber)!.blocked) {
            partnerRow = other.rowNumber;
            partnerGender = other.gender;
            partnerId = states.get(other.rowNumber)!.playerId;
            partnerMatch = { result: 'EXACT', selectedPlayerId: partnerId, candidates: [] };
            const namesBack = other.entries.some(
              (e) =>
                e.eventId === event.id &&
                e.partner.kind === 'NAMED' &&
                rowNamed(e.partner.name, other.rowNumber)?.rowNumber === row.rowNumber,
            );
            const otherInEvent = other.entries.some((e) => e.eventId === event.id);
            const otherEntry =
              other.rowNumber < row.rowNumber
                ? entries.get(other.rowNumber)?.find((e) => e.report.eventId === event.id)
                : undefined;
            if (namesBack && otherEntry && otherEntry.report.status === null) {
              partnerRow = null;
              partnerId = null;
              partnerGender = null;
              status = 'LOOKING_FOR_PARTNER';
              issues.push(issue('PARTNER_UNRESOLVED', `"${name}" cannot play this event`));
            } else if (namesBack) {
              status = 'CONFIRMED';
              if (otherEntry) coveredByRow = other.rowNumber;
            } else if (otherInEvent) {
              partnerRow = null;
              partnerId = null;
              partnerGender = null;
              status = 'LOOKING_FOR_PARTNER';
              issues.push(issue('PARTNER_UNRESOLVED', `"${name}" registered with someone else`));
            } else {
              status = 'PENDING_PARTNER';
            }
          } else {
            partnerMatch = classify(candidatesFor(name), false);
            const chosen =
              decision?.action === 'USE_EXISTING'
                ? decision.playerId
                : partnerMatch.result === 'EXACT' || partnerMatch.result === 'PROBABLE'
                  ? partnerMatch.selectedPlayerId
                  : null;
            if (decision?.action === 'USE_EXISTING') {
              partnerMatch = { ...partnerMatch, selectedPlayerId: decision.playerId };
            }
            const partner = db.players.find((p) => p.id === chosen);
            if (partner) {
              partnerId = partner.id;
              partnerGender = partner.gender;
              status = 'PENDING_PARTNER';
            } else {
              status = 'LOOKING_FOR_PARTNER';
              issues.push(issue('PARTNER_UNRESOLVED', `Partner "${name}" not found`));
            }
          }
        }

        const gender = requiredGender(event.discipline);
        const registered = (playerId: string | null) =>
          playerId !== null &&
          db.registrations.some(
            (r) =>
              r.eventId === event.id && (r.player.id === playerId || r.partner?.id === playerId),
          );
        const tournament = db.tournaments.find((t) => t.id === tournamentId);
        const forbidden = (tournament?.forbiddenCombinations ?? []).some(
          ([a, b]) =>
            (a === event.discipline && acceptedDisciplines.includes(b)) ||
            (b === event.discipline && acceptedDisciplines.includes(a)),
        );
        const count = db.registrations.filter((r) => r.eventId === event.id).length;

        let blocking: ImportIssue | null = null;
        if (gender && row.gender !== gender) {
          blocking = issue('PLAYER_NOT_ELIGIBLE', 'Wrong gender for this event');
        } else if (
          event.discipline === 'XD' &&
          partnerGender !== null &&
          partnerGender === row.gender
        ) {
          blocking = issue('PARTNER_NOT_ELIGIBLE', 'Mixed doubles needs a man and a woman');
        } else if (registered(state.playerId)) {
          blocking = issue('ALREADY_REGISTERED', 'Already registered in this event');
        } else if (registered(partnerId)) {
          blocking = issue('PARTNER_ALREADY_REGISTERED', 'Partner already registered');
        } else if (forbidden) {
          blocking = issue('FORBIDDEN_COMBINATION', 'Forbidden combination');
        } else if (
          coveredByRow === null &&
          event.maxEntries !== null &&
          count + (planned.get(event.id) ?? 0) >= event.maxEntries
        ) {
          blocking = issue('EVENT_FULL', 'This event is full');
        }

        if (blocking) {
          issues.push(blocking);
          status = null;
        } else {
          acceptedDisciplines.push(event.discipline);
          if (coveredByRow === null) planned.set(event.id, (planned.get(event.id) ?? 0) + 1);
        }
      }

      rowEntries.push({
        report: { eventId: entry.eventId, status, partnerMatch, issues },
        coveredByRow,
        partnerRow,
        partnerId,
      });
    }
    entries.set(row.rowNumber, rowEntries);
  }

  return { states, entries, findEvent };
}

function createPlayers(states: Map<number, RowState>) {
  const players = new Map<number, Player>();
  for (const state of states.values()) {
    if (state.blocked) continue;
    const { row } = state;
    let club: Club | undefined = db.clubs.find(
      (c) => normalize(c.name) === normalize(row.clubName),
    );
    if (!club) {
      club = {
        id: crypto.randomUUID(),
        name: row.clubName.trim(),
        city: row.clubCity!.trim(),
        address: null,
        email: null,
        phone: null,
        logoUrl: null,
      };
      db.clubs.push(club);
    }
    let player = db.players.find((p) => p.id === state.playerId);
    if (!player) {
      player = {
        id: crypto.randomUUID(),
        firstName: row.firstName,
        lastName: row.lastName,
        gender: row.gender,
        birthDate: row.birthDate,
        club: { id: club.id, name: club.name, logoUrl: club.logoUrl },
        licenseNumber: null,
        email: row.email,
        phone: row.phone,
        rankings: {
          singles: { points: null, position: null },
          doubles: { points: null, position: null },
          mixed: { points: null, position: null },
        },
      };
      db.players.push(player);
    }
    players.set(row.rowNumber, player);
  }
  return players;
}

export const registrationImportHandlers = [
  http.post('/api/tournaments/:tournamentId/registrations/import', async ({ params, request }) => {
    await delay(600);
    const tournamentId = String(params.tournamentId);
    if (!db.tournaments.some((t) => t.id === tournamentId)) {
      return problem(404, 'TOURNAMENT_NOT_FOUND', 'Tournament not found');
    }
    const dryRun = new URL(request.url).searchParams.get('dryRun') === 'true';
    const { rows } = (await request.json()) as { rows: ImportRow[] };
    if (!Array.isArray(rows) || rows.length === 0) {
      return problem(400, 'VALIDATION_FAILED', 'Invalid request', [
        { field: 'rows', message: 'must not be empty' },
      ]);
    }

    const { states, entries, findEvent } = analyse(tournamentId, rows);

    if (!dryRun) {
      const players = createPlayers(states);
      for (const [rowNumber, rowEntries] of entries) {
        const player = players.get(rowNumber);
        if (!player) continue;
        for (const entry of rowEntries) {
          const event = findEvent(entry.report.eventId);
          if (!event || entry.report.status === null || entry.coveredByRow !== null) continue;
          const partner =
            entry.partnerRow !== null
              ? players.get(entry.partnerRow)
              : db.players.find((p) => p.id === entry.partnerId);
          const registration: Registration = {
            id: crypto.randomUUID(),
            tournamentId,
            eventId: event.id,
            status: entry.report.status,
            player: toPlayerSummary(player),
            partner: partner ? toPlayerSummary(partner) : null,
            createdAt: new Date().toISOString(),
          };
          db.registrations.push(registration);
        }
      }
    }

    const reportRows: ImportRowReport[] = [...states.values()].map((state) => ({
      rowNumber: state.row.rowNumber,
      imported: !dryRun && !state.blocked,
      playerMatch: state.match,
      entries: (entries.get(state.row.rowNumber) ?? []).map((e) => e.report),
      issues: state.issues,
    }));

    const counted = [...entries.values()].flat().filter((e) => e.coveredByRow === null);
    const countStatus = (status: RegistrationStatus) =>
      counted.filter((e) => e.report.status === status).length;
    const undecided = (state: RowState) =>
      !state.row.playerDecision &&
      (state.match.result === 'PROBABLE' || state.match.result === 'AMBIGUOUS');

    const report: ImportReport = {
      dryRun,
      summary: {
        rows: rows.length,
        rowsWithIssues: reportRows.filter(
          (r) => r.issues.length > 0 || r.entries.some((e) => e.issues.length > 0),
        ).length,
        playersMatched: [...states.values()].filter((s) => s.match.result === 'EXACT').length,
        playersToCreate: [...states.values()].filter((s) => !s.blocked && s.playerId === null)
          .length,
        playersToConfirm: [...states.values()].filter(undecided).length,
        confirmed: countStatus('CONFIRMED'),
        pendingPartner: countStatus('PENDING_PARTNER'),
        lookingForPartner: countStatus('LOOKING_FOR_PARTNER'),
      },
      rows: reportRows,
    };
    return HttpResponse.json(report);
  }),
];
