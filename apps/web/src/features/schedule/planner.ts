import type {
  Court,
  Event,
  Match,
  MatchStatus,
  Registration,
  ScheduleEventSetting,
  ScheduleStage,
  ScheduleWarning,
  Tournament,
} from '@/api/models';

export const PLANNABLE: MatchStatus[] = ['SCHEDULED', 'READY'];
export const ONGOING: MatchStatus[] = ['CALLED', 'WARMUP', 'IN_PROGRESS'];
const MINUTE = 60_000;
const MAX_DAYS = 7;

export type PlannerInput = {
  tournament: Pick<Tournament, 'startsAt' | 'minimumRestMinutes' | 'averageMatchMinutes'>;
  events: Event[];
  courts: Court[];
  registrations: Registration[];
  matches: Match[];
  eventSettings?: ScheduleEventSetting[];
};

export type Placement = { matchId: string; plannedStartAt: string; plannedCourtId: string };

const PHASE_ORDER = { POOL: 0, KNOCKOUT: 1, THIRD_PLACE: 1 } as const;

const STAGE_DEPTH: Record<ScheduleStage, number> = {
  KNOCKOUT: Number.POSITIVE_INFINITY,
  QUARTER: 2,
  SEMI: 1,
  FINAL: 0,
};

export function inStage(match: Match, stage: ScheduleStage, lastRound: number) {
  if (match.phase === 'THIRD_PLACE') return true;
  if (match.phase !== 'KNOCKOUT' || match.round === null) return false;
  return lastRound - match.round <= STAGE_DEPTH[stage];
}

export function playersByRegistration(registrations: Registration[]) {
  return new Map(
    registrations.map((r) => [r.id, [r.player.id, ...(r.partner ? [r.partner.id] : [])]]),
  );
}

export function matchPlayers(match: Match, players: Map<string, string[]>) {
  return (['A', 'B'] as const).flatMap((which) => {
    const side = which === 'A' ? match.sideA : match.sideB;
    if (side) return players.get(side) ?? [];
    const source = which === 'A' ? match.sourceA : match.sourceB;
    if (!source) return [];
    return source.kind === 'POOL_RANK'
      ? [`${match.eventId}:${source.pool}:${source.rank}`]
      : [`${source.kind}:${source.matchId}`];
  });
}

export function dependenciesOf(match: Match, matches: Match[]) {
  const ids = new Set<string>();
  for (const source of [match.sourceA, match.sourceB]) {
    if (!source) continue;
    if (source.kind === 'POOL_RANK') {
      for (const m of matches) {
        if (m.eventId === match.eventId && m.phase === 'POOL' && m.pool === source.pool) {
          ids.add(m.id);
        }
      }
    } else {
      ids.add(source.matchId);
    }
  }
  return [...ids];
}

function slotOf(event: Event | undefined, fallback: number) {
  const start = event?.plannedDate ? Date.parse(event.plannedDate) : fallback;
  const end = event?.plannedEndDate ? Date.parse(event.plannedEndDate) : null;
  return { start, end };
}

export function planSchedule({
  tournament,
  events,
  courts,
  registrations,
  matches,
  eventSettings = [],
}: PlannerInput): Placement[] {
  const duration = (tournament.averageMatchMinutes ?? 25) * MINUTE;
  const rest = tournament.minimumRestMinutes * MINUTE;
  const tournamentStart = Date.parse(tournament.startsAt);
  const eventsById = new Map(events.map((e) => [e.id, e]));
  const players = playersByRegistration(registrations);
  const activeCourts = courts.filter((c) => c.active).sort((a, b) => a.number - b.number);

  const settings = new Map(eventSettings.map((s) => [s.eventId, s]));
  const priority = new Map(eventSettings.map((s, index) => [s.eventId, index]));
  const lastRound = new Map<string, number>();
  for (const m of matches) {
    if (m.phase === 'KNOCKOUT' && m.round !== null) {
      lastRound.set(m.eventId, Math.max(lastRound.get(m.eventId) ?? 0, m.round));
    }
  }
  const slot = (match: Match) => {
    const setting = settings.get(match.eventId);
    const base = slotOf(eventsById.get(match.eventId), tournamentStart);
    const start = setting?.startAt ? Date.parse(setting.startAt) : base.start;
    return { ...base, start: start + (setting?.offsetSlots ?? 0) * duration };
  };
  const earliest = (match: Match) => {
    let time = slot(match).start;
    for (const rule of settings.get(match.eventId)?.stageStarts ?? []) {
      if (inStage(match, rule.stage, lastRound.get(match.eventId) ?? 0)) {
        time = Math.max(time, Date.parse(rule.at));
      }
    }
    return time;
  };
  const rank = (match: Match) => priority.get(match.eventId) ?? Number.MAX_SAFE_INTEGER;
  const remaining = matches
    .filter((m) => PLANNABLE.includes(m.status))
    .sort(
      (a, b) =>
        slot(a).start - slot(b).start ||
        rank(a) - rank(b) ||
        PHASE_ORDER[a.phase ?? 'POOL'] - PHASE_ORDER[b.phase ?? 'POOL'] ||
        (a.round ?? 0) - (b.round ?? 0) ||
        (a.number ?? 0) - (b.number ?? 0),
    );
  if (remaining.length === 0 || activeCourts.length === 0) return [];

  const plannedEnd = new Map<string, number>();
  const playerFree = new Map<string, number>();
  const placements: Placement[] = [];
  const dependencies = new Map(remaining.map((m) => [m.id, dependenciesOf(m, matches)]));
  const plannableIds = new Set(remaining.map((m) => m.id));

  const readyAt = (match: Match) => {
    let time = earliest(match);
    for (const id of dependencies.get(match.id) ?? []) {
      if (!plannableIds.has(id) && !plannedEnd.has(id)) continue;
      const end = plannedEnd.get(id);
      if (end === undefined) return Number.POSITIVE_INFINITY;
      time = Math.max(time, end + rest);
    }
    for (const player of matchPlayers(match, players)) {
      const free = playerFree.get(player);
      if (free !== undefined) time = Math.max(time, free + rest);
    }
    return time;
  };

  let time = Math.min(...remaining.map((m) => slot(m).start));
  const limit = time + MAX_DAYS * 24 * 60 * MINUTE;
  const ongoing = matches.filter((m) => ONGOING.includes(m.status));
  const firstSlot = time;
  for (const match of ongoing) {
    plannedEnd.set(match.id, firstSlot);
    for (const player of matchPlayers(match, players)) playerFree.set(player, firstSlot);
  }
  const occupied = new Set(ongoing.map((m) => m.courtId));

  while (remaining.length > 0 && time <= limit) {
    const busy = new Set<string>();
    const placedBefore = placements.length;
    for (const court of activeCourts) {
      if (time === firstSlot && occupied.has(court.id)) continue;
      const index = remaining.findIndex((m) => {
        if (readyAt(m) > time) return false;
        return !matchPlayers(m, players).some((p) => busy.has(p));
      });
      if (index === -1) continue;
      const [match] = remaining.splice(index, 1);
      for (const player of matchPlayers(match, players)) {
        busy.add(player);
        playerFree.set(player, time + duration);
      }
      plannedEnd.set(match.id, time + duration);
      placements.push({
        matchId: match.id,
        plannedStartAt: new Date(time).toISOString(),
        plannedCourtId: court.id,
      });
    }
    const running = placements.some((p) => Date.parse(p.plannedStartAt) + duration > time);
    const next = Math.min(...remaining.map(readyAt));
    time =
      placements.length === placedBefore && !running && Number.isFinite(next) && next > time
        ? next
        : time + duration;
  }

  return placements;
}

export function checkSchedule({
  tournament,
  events,
  registrations,
  matches,
}: Omit<PlannerInput, 'courts'>): ScheduleWarning[] {
  const duration = (tournament.averageMatchMinutes ?? 25) * MINUTE;
  const rest = tournament.minimumRestMinutes * MINUTE;
  const players = playersByRegistration(registrations);
  const byId = new Map(matches.map((m) => [m.id, m]));
  const warnings: ScheduleWarning[] = [];
  const planned = matches.filter((m) => m.plannedStartAt);
  const startOf = (m: Match) => Date.parse(m.plannedStartAt ?? '');

  for (const event of events) {
    const eventMatches = planned.filter((m) => m.eventId === event.id);
    if (eventMatches.length === 0) continue;
    if (!event.plannedDate) {
      warnings.push({ code: 'NO_SLOT', eventId: event.id });
      continue;
    }
    const { start, end } = slotOf(event, 0);
    const count = eventMatches.filter(
      (m) => startOf(m) < start || (end !== null && startOf(m) + duration > end),
    ).length;
    if (count > 0) warnings.push({ code: 'OUTSIDE_SLOT', eventId: event.id, count });
  }

  const byPlayer = new Map<string, Match[]>();
  for (const match of planned) {
    for (const player of matchPlayers(match, players)) {
      byPlayer.set(player, [...(byPlayer.get(player) ?? []), match]);
    }
  }
  const tooShort = new Set<string>();
  for (const list of byPlayer.values()) {
    list.sort((a, b) => startOf(a) - startOf(b));
    for (let i = 1; i < list.length; i++) {
      if (startOf(list[i]) - (startOf(list[i - 1]) + duration) < rest) tooShort.add(list[i].id);
    }
  }
  for (const matchId of tooShort) warnings.push({ code: 'REST_TOO_SHORT', matchId });

  for (const match of planned) {
    const late = dependenciesOf(match, matches).some((id) => {
      const dependency = byId.get(id);
      return (
        dependency?.plannedStartAt &&
        PLANNABLE.includes(dependency.status) &&
        startOf(dependency) + duration > startOf(match)
      );
    });
    if (late) warnings.push({ code: 'ORDER', matchId: match.id });
  }

  const unscheduled = matches.filter(
    (m) => PLANNABLE.includes(m.status) && !m.plannedStartAt,
  ).length;
  if (unscheduled > 0) warnings.push({ code: 'UNSCHEDULED', count: unscheduled });

  return warnings;
}
