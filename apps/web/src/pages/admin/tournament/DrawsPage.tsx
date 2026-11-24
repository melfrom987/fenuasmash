import { useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { toDrawEntries } from '@/features/draw/algorithm';
import { useCreateDraw, useDraw } from '@/features/draw/api';
import { DrawSettingsForm } from '@/features/draw/components/DrawSettingsForm';
import { DrawView } from '@/features/draw/components/DrawView';
import { useEvents } from '@/features/events/api';
import { useTournamentRegistrations } from '@/features/registrations/api';
import { EventPicker } from '@/features/registrations/components/EventPicker';
import { useSplitIntoSeries } from '@/features/series/api';
import { SeriesPanel } from '@/features/series/components/SeriesPanel';
import { SeriesSplitForm } from '@/features/series/components/SeriesSplitForm';
import { MIN_SERIES_ENTRIES } from '@/features/series/naming';
import { useTournament } from '@/features/tournaments/api';
import { useErrorMessage } from '@/lib/use-error-message';

export function DrawsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const tournament = useTournament(tournamentId);
  const events = useEvents(tournamentId);
  const registrations = useTournamentRegistrations(tournamentId);
  const [searchParams, setSearchParams] = useSearchParams();
  const [editing, setEditing] = useState(false);
  const [splitting, setSplitting] = useState(false);

  const selectedId = searchParams.get('event') ?? events.data?.[0]?.id ?? '';
  const event = events.data?.find((e) => e.id === selectedId);
  const draw = useDraw(event?.id ?? '');
  const createDraw = useCreateDraw(event?.id ?? '');
  const splitIntoSeries = useSplitIntoSeries(tournamentId, event?.id ?? '');
  const format = tournament.data?.drawFormat ?? 'POOLS_THEN_ELIMINATION';

  const confirmedCounts = useMemo(() => {
    const map = new Map<string, number>();
    for (const r of registrations.data ?? []) {
      if (r.status === 'CONFIRMED') map.set(r.eventId, (map.get(r.eventId) ?? 0) + 1);
    }
    return map;
  }, [registrations.data]);

  const eventRegistrations = (registrations.data ?? []).filter((r) => r.eventId === selectedId);
  const confirmedRegistrations = eventRegistrations.filter((r) => r.status === 'CONFIRMED');
  const confirmed = confirmedRegistrations.length;

  const select = (eventId: string) => {
    setSearchParams({ event: eventId });
    setEditing(false);
    setSplitting(false);
    createDraw.reset();
    splitIntoSeries.reset();
  };

  if (events.isPending) return <p className="text-sable-600">{t('common.loading')}</p>;
  if (events.isError) return <ErrorMessage>{errorMessage(events.error)}</ErrorMessage>;
  if (events.data.length === 0)
    return <p className="text-sable-600">{t('registrations.noEvents')}</p>;

  const hasSeries = events.data.some((e) => e.parentEventId === event?.id);
  const canSplit =
    event !== undefined &&
    event.parentEventId === null &&
    draw.data?.status !== 'PUBLISHED' &&
    confirmed >= MIN_SERIES_ENTRIES * 2;

  return (
    <div className="grid items-start gap-6 lg:grid-cols-[16rem_minmax(0,1fr)]">
      <EventPicker
        events={events.data}
        counts={confirmedCounts}
        selectedId={selectedId}
        onSelect={select}
      />

      {event && (
        <div className="min-w-0 space-y-4">
          <h2 className="font-display text-lg font-semibold">{event.name}</h2>

          {!splitting && (
            <SeriesPanel
              event={event}
              events={events.data}
              counts={confirmedCounts}
              canSplit={canSplit}
              onSplit={() => setSplitting(true)}
              onSelect={select}
            />
          )}

          {splitting && (
            <SeriesSplitForm
              event={event}
              entries={toDrawEntries(confirmedRegistrations, event.discipline)}
              excludedCount={eventRegistrations.length - confirmed}
              pending={splitIntoSeries.isPending}
              error={splitIntoSeries.error ? errorMessage(splitIntoSeries.error) : null}
              onSubmit={(series) =>
                splitIntoSeries.mutate(
                  { series },
                  { onSuccess: (result) => select(result.events[0].id) },
                )
              }
              onCancel={() => setSplitting(false)}
            />
          )}

          {!splitting && draw.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
          {!splitting && draw.isError && <ErrorMessage>{errorMessage(draw.error)}</ErrorMessage>}

          {!splitting &&
            draw.data !== undefined &&
            (draw.data === null || editing) &&
            !(hasSeries && confirmed < 2) && (
              <DrawSettingsForm
                key={event.id}
                format={format}
                entryCount={confirmed}
                excludedCount={eventRegistrations.length - confirmed}
                defaults={{
                  poolCount: event.poolCount,
                  qualifiersPerPool: event.qualifiersPerPool,
                  seedCount: draw.data?.pools.length === 0 ? draw.data.seedCount : null,
                }}
                pending={createDraw.isPending}
                error={createDraw.error ? errorMessage(createDraw.error) : null}
                onSubmit={(settings) =>
                  createDraw.mutate(settings, { onSuccess: () => setEditing(false) })
                }
                onCancel={draw.data ? () => setEditing(false) : undefined}
              />
            )}

          {!splitting && draw.data && !editing && (
            <DrawView draw={draw.data} onEditSettings={() => setEditing(true)} />
          )}
        </div>
      )}
    </div>
  );
}
