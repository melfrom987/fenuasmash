import type { Court, Draw, Event, Match, Registration, Schedule } from '@/api/models';
import { MIN_EVENT_ENTRIES } from '@/features/draw/algorithm';
import { ON_COURT } from '@/features/matches/status';

export type StepId = 'prepare' | 'registrations' | 'draws' | 'schedule' | 'matchday';
export type StepState = 'done' | 'current' | 'todo' | 'soon';

export type NextAction =
  | { step: 'prepare'; kind: 'addCourts' | 'addEvents'; to: string }
  | { step: 'registrations'; kind: 'register'; to: string; count: number }
  | { step: 'draws'; kind: 'runDraw' | 'publishDraw' | 'moveEntries'; to: string; event: string }
  | {
      step: 'schedule';
      kind: 'generateSchedule' | 'updateSchedule' | 'publishSchedule';
      to: string;
    }
  | { step: 'matchday'; kind: 'startMatches'; to: string };

export type StepPage = {
  path: string;
  label: 'settings' | 'events' | 'distribution' | 'draws' | 'matches' | 'tablets';
};

export const STEPS: { id: StepId; pages: StepPage[] }[] = [
  {
    id: 'prepare',
    pages: [
      { path: 'settings', label: 'settings' },
      { path: 'events', label: 'events' },
    ],
  },
  { id: 'registrations', pages: [] },
  {
    id: 'draws',
    pages: [
      { path: 'distribution', label: 'distribution' },
      { path: 'draws', label: 'draws' },
    ],
  },
  { id: 'schedule', pages: [] },
  {
    id: 'matchday',
    pages: [
      { path: 'matches', label: 'matches' },
      { path: 'tablets', label: 'tablets' },
    ],
  },
];

export const STEP_PATHS: Record<StepId, string[]> = {
  prepare: ['settings', 'events'],
  registrations: ['registrations'],
  draws: ['distribution', 'draws'],
  schedule: ['schedule'],
  matchday: ['matches', 'tablets'],
};

export const SOON_STEPS: StepId[] = [];

export const MIN_ENTRIES = MIN_EVENT_ENTRIES;

export type ProgressInput = {
  courts: Court[];
  events: Event[];
  registrations: Registration[];
  draws: Map<string, Draw | null>;
  matches: Match[];
  schedule: Schedule | null;
};

export type StepSummary =
  | { step: 'prepare'; courts: number; events: number }
  | { step: 'registrations'; entries: number }
  | { step: 'draws'; published: number; total: number }
  | { step: 'schedule'; status: Schedule['status'] | null; planned: number }
  | { step: 'matchday'; live: number; total: number };

export type Progress = {
  states: Record<StepId, StepState>;
  summaries: StepSummary[];
  next: NextAction | null;
  drawableEventIds: string[];
};

export function stepOf(path: string): StepId {
  const first = path.split('/')[0];
  const found = (Object.keys(STEP_PATHS) as StepId[]).find((id) => STEP_PATHS[id].includes(first));
  return found ?? 'prepare';
}

export function drawableEvents(events: Event[]) {
  const parents = new Set(events.map((e) => e.parentEventId).filter((id) => id !== null));
  return events.filter((e) => !parents.has(e.id));
}

export function computeProgress({
  courts,
  events,
  registrations,
  draws,
  matches,
  schedule,
}: ProgressInput): Progress {
  const confirmed = new Map<string, number>();
  for (const r of registrations) {
    if (r.status === 'CONFIRMED') confirmed.set(r.eventId, (confirmed.get(r.eventId) ?? 0) + 1);
  }
  const activeCourts = courts.filter((c) => c.active).length;
  const playable = drawableEvents(events);
  const ready = playable.filter((e) => (confirmed.get(e.id) ?? 0) >= MIN_ENTRIES);
  const published = ready.filter((e) => draws.get(e.id)?.status === 'PUBLISHED');
  const live = matches.filter((m) => ON_COURT.includes(m.status)).length;

  const prepareDone = activeCourts > 0 && events.length > 0;
  const registrationsDone = ready.length > 0;
  const drawsDone = ready.length > 0 && published.length === ready.length;

  const entries = new Map<string, number>();
  for (const r of registrations) entries.set(r.eventId, (entries.get(r.eventId) ?? 0) + 1);
  const underfilled = playable.find(
    (e) => (confirmed.get(e.id) ?? 0) < MIN_ENTRIES && (entries.get(e.id) ?? 0) > 0,
  );
  let next: NextAction | null = null;
  if (activeCourts === 0) next = { step: 'prepare', kind: 'addCourts', to: 'settings' };
  else if (events.length === 0) next = { step: 'prepare', kind: 'addEvents', to: 'events' };
  else if (!registrationsDone) {
    const missing = playable.find((e) => (confirmed.get(e.id) ?? 0) < MIN_ENTRIES);
    next = {
      step: 'registrations',
      kind: 'register',
      to: missing ? `registrations?event=${missing.id}` : 'registrations',
      count: playable.length,
    };
  } else if (underfilled) {
    next = {
      step: 'draws',
      kind: 'moveEntries',
      to: `draws?event=${underfilled.id}`,
      event: underfilled.name,
    };
  } else if (!drawsDone) {
    const event = ready.find((e) => draws.get(e.id)?.status !== 'PUBLISHED');
    if (event) {
      next = {
        step: 'draws',
        kind: draws.get(event.id) ? 'publishDraw' : 'runDraw',
        to: `draws?event=${event.id}`,
        event: event.name,
      };
    }
  } else if (!schedule || schedule.status === 'NOT_GENERATED') {
    next = { step: 'schedule', kind: 'generateSchedule', to: 'schedule' };
  } else if (schedule.warnings.some((w) => w.code === 'UNSCHEDULED')) {
    next = { step: 'schedule', kind: 'updateSchedule', to: 'schedule' };
  } else if (schedule.status === 'DRAFT') {
    next = { step: 'schedule', kind: 'publishSchedule', to: 'schedule' };
  } else if (live === 0) {
    next = { step: 'matchday', kind: 'startMatches', to: 'matches' };
  }

  const done: Record<StepId, boolean> = {
    prepare: prepareDone,
    registrations: registrationsDone,
    draws: drawsDone,
    schedule: schedule?.status === 'PUBLISHED',
    matchday: false,
  };
  const states = {} as Record<StepId, StepState>;
  for (const { id } of STEPS) {
    states[id] = SOON_STEPS.includes(id)
      ? 'soon'
      : next?.step === id
        ? 'current'
        : done[id]
          ? 'done'
          : 'todo';
  }
  if (!next) states.matchday = 'current';

  return {
    states,
    next,
    drawableEventIds: ready.map((e) => e.id),
    summaries: [
      { step: 'prepare', courts: activeCourts, events: events.length },
      {
        step: 'registrations',
        entries: registrations.filter((r) => r.status === 'CONFIRMED').length,
      },
      { step: 'draws', published: published.length, total: ready.length },
      {
        step: 'schedule',
        status: schedule?.status ?? null,
        planned: matches.filter((m) => m.plannedStartAt).length,
      },
      { step: 'matchday', live, total: matches.length },
    ],
  };
}
