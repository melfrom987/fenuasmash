import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { Match } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { secondaryButtonClass } from '@/components/ui/styles';
import { useCourts } from '@/features/courts/api';
import { useEvents } from '@/features/events/api';
import { tournamentMatchesQuery } from '@/features/matches/api';
import { useMatchLabels } from '@/features/matches/use-match-labels';
import {
  useGenerateSchedule,
  useMoveInSchedule,
  usePublishSchedule,
  useSchedule,
} from '@/features/schedule/api';
import { eventColors } from '@/features/schedule/colors';
import { EventOrderPanel } from '@/features/schedule/components/EventOrderPanel';
import { ScheduleCell } from '@/features/schedule/components/ScheduleCell';
import { ScheduleGrid } from '@/features/schedule/components/ScheduleGrid';
import { ScheduleWarnings } from '@/features/schedule/components/ScheduleWarnings';
import { buildGrid, gridCourts } from '@/features/schedule/grid';
import { PLANNABLE } from '@/features/schedule/planner';
import { useTournament } from '@/features/tournaments/api';
import { SCHEDULE_DEFAULTS } from '@/features/tournaments/schedule-settings';
import { formatDate, formatDateTime, formatTime } from '@/lib/dates';
import { useErrorMessage } from '@/lib/use-error-message';

const STATUS_CLASSES = {
  NOT_GENERATED: 'bg-sable-100 text-sable-700',
  DRAFT: 'bg-sunset-100 text-sunset-800',
  PUBLISHED: 'bg-lagon-100 text-lagon-800',
} as const;

export function SchedulePage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const [selected, setSelected] = useState<Match | null>(null);
  const [dayKey, setDayKey] = useState<string | null>(null);
  const [confirmRegenerate, setConfirmRegenerate] = useState(false);
  const [focusedEventId, setFocusedEventId] = useState<string | null>(null);
  const events = useEvents(tournamentId);

  const tournament = useTournament(tournamentId);
  const courts = useCourts(tournamentId);
  const matches = useQuery(tournamentMatchesQuery(tournamentId));
  const schedule = useSchedule(tournamentId);
  const labels = useMatchLabels(tournamentId);
  const generate = useGenerateSchedule(tournamentId);
  const move = useMoveInSchedule(tournamentId);
  const publish = usePublishSchedule(tournamentId);

  if (tournament.isPending || courts.isPending || matches.isPending || schedule.isPending) {
    return <p className="text-sable-600">{t('common.loading')}</p>;
  }
  const loadError = tournament.error ?? courts.error ?? matches.error ?? schedule.error;
  if (loadError || !tournament.data || !courts.data || !matches.data || !schedule.data) {
    return <ErrorMessage>{errorMessage(loadError ?? new Error())}</ErrorMessage>;
  }

  const status = schedule.data.status;
  const average = tournament.data.averageMatchMinutes ?? SCHEDULE_DEFAULTS.averageMatchMinutes;
  const allMatches = matches.data;
  const plannable = allMatches.filter((m) => PLANNABLE.includes(m.status));
  const unscheduled = plannable.filter((m) => !m.plannedStartAt);
  const columns = gridCourts(allMatches, courts.data);
  const days = buildGrid(allMatches, columns);
  const day = days.find((d) => d.key === dayKey) ?? days[0];
  const lastRow = day?.rows.at(-1);
  const extraStart = lastRow
    ? new Date(Date.parse(lastRow.start) + average * 60_000).toISOString()
    : '';
  const flagged = new Set(
    schedule.data.warnings.flatMap((w) =>
      w.code === 'REST_TOO_SHORT' || w.code === 'ORDER' ? [w.matchId] : [],
    ),
  );
  const colors = eventColors((events.data ?? []).map((e) => e.id));
  const actionError = generate.error ?? move.error ?? publish.error;
  const pending = generate.isPending || move.isPending || publish.isPending;

  const place = (start: string, courtId: string) => {
    if (!selected) return;
    move.mutate(
      { matchId: selected.id, plannedStartAt: start, courtId },
      { onSuccess: () => setSelected(null) },
    );
  };

  const runGenerate = () => {
    setConfirmRegenerate(false);
    setSelected(null);
    generate.mutate();
  };

  const settingsPath = `/admin/tournaments/${tournamentId}/settings`;

  return (
    <div className="space-y-5">
      <div>
        <h2 className="font-display text-lg font-semibold">{t('schedule.title')}</h2>
        <p className="text-sable-600 text-sm">{t('schedule.pageHint')}</p>
      </div>

      <section className="flex flex-wrap items-start justify-between gap-4 rounded-lg bg-white p-4 shadow-sm">
        <div className="space-y-1 text-sm">
          <p className="flex flex-wrap items-center gap-2">
            <span className={`rounded px-2 py-0.5 text-xs font-medium ${STATUS_CLASSES[status]}`}>
              {t(`schedule.statuses.${status}`)}
            </span>
            {schedule.data.publishedAt && (
              <span className="text-sable-600">
                {t('schedule.publishedOn', { date: formatDateTime(schedule.data.publishedAt) })}
              </span>
            )}
          </p>
          {schedule.data.firstStartAt && schedule.data.lastEndAt && (
            <p>
              {t('schedule.span', {
                count: allMatches.filter((m) => m.plannedStartAt).length,
                from: `${formatDate(schedule.data.firstStartAt)} ${formatTime(schedule.data.firstStartAt)}`,
                to: formatTime(schedule.data.lastEndAt),
              })}
            </p>
          )}
          <p className="text-sable-600">
            {t('schedule.settingsSummary', {
              average,
              rest: tournament.data.minimumRestMinutes,
              courts: courts.data.filter((c) => c.active).length,
            })}{' '}
            <Link to={settingsPath} className="text-lagon-700 hover:underline">
              {t('schedule.editSettings')}
            </Link>
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {confirmRegenerate ? (
            <>
              <span className="text-sm">{t('schedule.regenerateConfirm')}</span>
              <button
                type="button"
                onClick={() => setConfirmRegenerate(false)}
                className="text-sable-600 text-sm hover:underline"
              >
                {t('common.cancel')}
              </button>
              <Button type="button" disabled={pending} onClick={runGenerate}>
                {t('schedule.regenerate')}
              </Button>
            </>
          ) : (
            <>
              {status === 'NOT_GENERATED' ? (
                <Button
                  type="button"
                  disabled={pending || plannable.length === 0}
                  onClick={runGenerate}
                >
                  {generate.isPending ? t('schedule.generating') : t('schedule.generate')}
                </Button>
              ) : (
                <button
                  type="button"
                  disabled={pending || plannable.length === 0}
                  onClick={() => setConfirmRegenerate(true)}
                  className={secondaryButtonClass}
                >
                  {t('schedule.regenerate')}
                </button>
              )}
              {status === 'DRAFT' && (
                <Button type="button" disabled={pending} onClick={() => publish.mutate()}>
                  {t('schedule.publish')}
                </Button>
              )}
            </>
          )}
        </div>
      </section>

      {actionError && <ErrorMessage>{errorMessage(actionError)}</ErrorMessage>}

      {plannable.length === 0 && status === 'NOT_GENERATED' && (
        <p className="text-sable-600 rounded-lg bg-white p-6 text-center text-sm shadow-sm">
          {t('schedule.noMatches')}{' '}
          <Link
            to={`/admin/tournaments/${tournamentId}/draws`}
            className="text-lagon-700 hover:underline"
          >
            {t('schedule.goToDraws')}
          </Link>
        </p>
      )}

      <ScheduleWarnings warnings={schedule.data.warnings} matches={allMatches} labels={labels} />

      {selected && (
        <div className="bg-lagon-50 border-lagon-200 flex flex-wrap items-center justify-between gap-2 rounded-md border px-4 py-2 text-sm">
          <span>{t('schedule.moveHint', { number: selected.number ?? '?' })}</span>
          <button
            type="button"
            onClick={() => setSelected(null)}
            className="text-lagon-700 hover:underline"
          >
            {t('common.cancel')}
          </button>
        </div>
      )}

      {status !== 'NOT_GENERATED' && unscheduled.length > 0 && (
        <section className="space-y-2">
          <h3 className="text-sm font-semibold">
            {t('schedule.unscheduledTitle', { count: unscheduled.length })}
          </h3>
          <ul className="grid gap-2 sm:grid-cols-3 lg:grid-cols-5">
            {unscheduled.map((match) => (
              <li key={match.id}>
                <ScheduleCell
                  match={match}
                  labels={labels}
                  selected={match.id === selected?.id}
                  flagged={false}
                  disabled={pending}
                  color={colors.get(match.eventId)}
                  onClick={() => setSelected(match.id === selected?.id ? null : match)}
                />
              </li>
            ))}
          </ul>
        </section>
      )}

      {status !== 'NOT_GENERATED' && schedule.data.events.length > 0 && (
        <EventOrderPanel
          key={`${schedule.data.generatedAt}-${schedule.data.events.map((e) => e.eventId).join()}`}
          events={schedule.data.events}
          eventName={labels.event}
          colors={colors}
          averageMinutes={average}
          focusedId={focusedEventId}
          pending={pending}
          onFocus={setFocusedEventId}
          onApply={(settings) => {
            setSelected(null);
            generate.mutate({ events: settings });
          }}
        />
      )}

      {day && (
        <section className="space-y-3">
          {days.length > 1 && (
            <div className="flex flex-wrap gap-2">
              {days.map((d) => (
                <button
                  key={d.key}
                  type="button"
                  onClick={() => setDayKey(d.key)}
                  className={`rounded-md px-3 py-1.5 text-sm capitalize ${
                    d.key === day.key
                      ? 'bg-ocean-900 text-white'
                      : 'text-sable-700 hover:bg-sable-100 bg-white'
                  }`}
                >
                  {formatDate(d.firstStart)}
                </button>
              ))}
            </div>
          )}
          <ScheduleGrid
            day={day}
            courts={columns}
            extraStart={extraStart}
            labels={labels}
            selectedId={selected?.id ?? null}
            flagged={flagged}
            pending={pending}
            colors={colors}
            focusedEventId={focusedEventId}
            onSelect={(match) => setSelected(match.id === selected?.id ? null : match)}
            onPlace={place}
          />
        </section>
      )}
    </div>
  );
}
