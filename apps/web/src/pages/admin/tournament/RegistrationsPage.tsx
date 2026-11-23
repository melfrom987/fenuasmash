import { useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { RegistrationStatus } from '@/api/models';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { inputClass } from '@/components/ui/styles';
import { useEvents } from '@/features/events/api';
import { seasonReferenceYear } from '@/features/players/age-category';
import { useTournamentRegistrations, useTransferRegistrations } from '@/features/registrations/api';
import { isOutOfCategory, suggestCategory } from '@/features/registrations/category-switch';
import { EventPicker } from '@/features/registrations/components/EventPicker';
import { RegistrationForm } from '@/features/registrations/components/RegistrationForm';
import { RegistrationRow } from '@/features/registrations/components/RegistrationRow';
import { TransferBar } from '@/features/registrations/components/TransferBar';
import { isDoubles, REGISTRATION_STATUSES } from '@/features/registrations/constants';
import { entryPoints, sortByRanking } from '@/features/registrations/ranking';
import { useTournament } from '@/features/tournaments/api';
import { ApiError } from '@/lib/api-client';
import { useErrorMessage } from '@/lib/use-error-message';

export function RegistrationsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const tournament = useTournament(tournamentId);
  const events = useEvents(tournamentId);
  const registrations = useTournamentRegistrations(tournamentId);
  const [status, setStatus] = useState<RegistrationStatus | ''>('');
  const [selected, setSelected] = useState<string[]>([]);
  const [switchReport, setSwitchReport] = useState<{ moved: number; errors: string[] } | null>(
    null,
  );
  const transfer = useTransferRegistrations(tournamentId);

  const [searchParams, setSearchParams] = useSearchParams();
  const selectedId = searchParams.get('event') ?? events.data?.[0]?.id;
  const event = events.data?.find((e) => e.id === selectedId);
  const referenceYear = seasonReferenceYear(
    tournament.data ? new Date(tournament.data.startsAt) : new Date(),
  );

  const counts = useMemo(() => {
    const map = new Map<string, number>();
    for (const r of registrations.data ?? []) map.set(r.eventId, (map.get(r.eventId) ?? 0) + 1);
    return map;
  }, [registrations.data]);

  const eventRegistrations = event
    ? sortByRanking(
        (registrations.data ?? []).filter(
          (r) => r.eventId === event.id && (!status || r.status === status),
        ),
        event.discipline,
      )
    : [];
  const outOfCategory = event
    ? eventRegistrations.filter((r) => isOutOfCategory(event, r)).map((r) => r.id)
    : [];
  const count = event ? (counts.get(event.id) ?? 0) : 0;
  const full = event !== undefined && event.maxEntries !== null && count >= event.maxEntries;
  const allSelected =
    eventRegistrations.length > 0 && eventRegistrations.every((r) => selected.includes(r.id));

  const suggestions = new Map(
    event && events.data
      ? outOfCategory.map((id) => {
          const registration = eventRegistrations.find((r) => r.id === id);
          return [id, registration && suggestCategory(registration, event, events.data)] as const;
        })
      : [],
  );
  const switchable = [...suggestions].filter(([, target]) => target !== undefined);

  const switchCategories = async (ids: string[]) => {
    const byTarget = new Map<string, string[]>();
    for (const id of ids) {
      const target = suggestions.get(id);
      if (target) byTarget.set(target.id, [...(byTarget.get(target.id) ?? []), id]);
    }
    let moved = 0;
    const errors: string[] = [];
    for (const [targetEventId, registrationIds] of byTarget) {
      try {
        const result = await transfer.mutateAsync({ targetEventId, registrationIds });
        moved += result.moved.length;
        for (const r of result.rejected) {
          const registration = eventRegistrations.find((x) => x.id === r.registrationId);
          const name = registration ? registration.player.lastName.toUpperCase() : '';
          errors.push(`${name} — ${errorMessage(new ApiError(400, r.message, r.code))}`);
        }
      } catch (error) {
        errors.push(errorMessage(error instanceof Error ? error : new Error(String(error))));
      }
    }
    setSelected([]);
    setSwitchReport({ moved, errors });
  };

  const toggle = (id: string) =>
    setSelected((list) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id]));

  if (events.isPending) return <p className="text-sable-600">{t('common.loading')}</p>;
  if (events.isError) return <ErrorMessage>{errorMessage(events.error)}</ErrorMessage>;
  if (events.data.length === 0) {
    return <p className="text-sable-600">{t('registrations.noEvents')}</p>;
  }

  return (
    <div className="max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-white p-4 shadow-sm">
        <p className="text-sable-700 text-sm">{t('registrations.importHint')}</p>
        <Link
          to="import"
          className="bg-lagon-600 hover:bg-lagon-700 rounded-md px-4 py-2 text-sm font-medium text-white"
        >
          {t('registrations.importLink')}
        </Link>
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-[16rem_minmax(0,1fr)]">
        <EventPicker
          events={events.data}
          counts={counts}
          selectedId={selectedId}
          onSelect={(eventId) => {
            setSearchParams({ event: eventId });
            setStatus('');
            setSelected([]);
            setSwitchReport(null);
          }}
        />

        {event && (
          <div className="space-y-6">
            <div>
              <h2 className="font-display text-lg font-semibold">{event.name}</h2>
              <p className="text-sable-600 text-sm">
                {event.maxEntries === null
                  ? t('registrations.count', { count })
                  : t('registrations.countOfMax', { count, max: event.maxEntries })}
              </p>
            </div>

            <section className="space-y-3">
              <div className="flex flex-wrap items-end justify-between gap-3">
                <div className="space-y-1">
                  <h3 className="font-display text-base font-semibold">
                    {t('registrations.listTitle')}
                  </h3>
                  <p className="text-sable-600 text-xs">{t('registrations.sortHint')}</p>
                </div>
                {isDoubles(event.discipline) && (
                  <label className="block">
                    <span className="sr-only">{t('registrations.filterStatus')}</span>
                    <select
                      value={status}
                      onChange={(e) => setStatus(e.target.value as RegistrationStatus | '')}
                      className={`${inputClass} mt-0 w-auto`}
                    >
                      <option value="">{t('registrations.allStatuses')}</option>
                      {REGISTRATION_STATUSES.map((s) => (
                        <option key={s} value={s}>
                          {t(`registrations.statuses.${s}`)}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
              </div>

              {eventRegistrations.length > 0 && (
                <div className="flex flex-wrap items-center gap-4 text-sm">
                  <label className="text-sable-800 flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={allSelected}
                      onChange={() =>
                        setSelected(allSelected ? [] : eventRegistrations.map((r) => r.id))
                      }
                      className="accent-lagon-600 size-4"
                    />
                    {t('registrations.selectAll')}
                  </label>
                  {outOfCategory.length > 0 && (
                    <button
                      type="button"
                      onClick={() => setSelected(outOfCategory)}
                      className="text-red-700 hover:underline"
                    >
                      {t('registrations.selectOutOfCategory', { count: outOfCategory.length })}
                    </button>
                  )}
                  {switchable.length > 0 && (
                    <button
                      type="button"
                      disabled={transfer.isPending}
                      onClick={() => void switchCategories(switchable.map(([id]) => id))}
                      className="text-lagon-700 font-medium hover:underline disabled:opacity-50"
                    >
                      {t('registrations.switchAll', { count: switchable.length })}
                    </button>
                  )}
                </div>
              )}

              {switchReport && (
                <div className="rounded-lg bg-white p-3 text-sm shadow-sm">
                  <div className="flex items-start justify-between gap-3">
                    <p className="text-lagon-800 font-medium">
                      {t('registrations.transfer.moved', { count: switchReport.moved })}
                    </p>
                    <button
                      type="button"
                      onClick={() => setSwitchReport(null)}
                      className="text-sable-600 text-xs hover:underline"
                    >
                      {t('registrations.transfer.dismiss')}
                    </button>
                  </div>
                  {switchReport.errors.length > 0 && (
                    <ul className="mt-1 space-y-1 text-red-800">
                      {switchReport.errors.map((message) => (
                        <li key={message}>{message}</li>
                      ))}
                    </ul>
                  )}
                </div>
              )}

              <TransferBar
                tournamentId={tournamentId}
                event={event}
                events={events.data}
                selected={selected}
                registrations={registrations.data ?? []}
                onClear={() => setSelected([])}
              />

              {registrations.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
              {registrations.isError && (
                <ErrorMessage>{errorMessage(registrations.error)}</ErrorMessage>
              )}
              {registrations.data && eventRegistrations.length === 0 && (
                <p className="text-sable-600 rounded-lg bg-white p-6 text-center shadow-sm">
                  {status ? t('registrations.noneWithStatus') : t('registrations.empty')}
                </p>
              )}
              {eventRegistrations.length > 0 && (
                <ul className="divide-sable-200 divide-y rounded-lg bg-white shadow-sm">
                  {eventRegistrations.map((registration, index) => (
                    <RegistrationRow
                      key={registration.id}
                      tournamentId={tournamentId}
                      event={event}
                      registration={registration}
                      rank={index + 1}
                      points={entryPoints(registration, event.discipline)}
                      outOfCategory={outOfCategory.includes(registration.id)}
                      referenceYear={referenceYear}
                      selected={selected.includes(registration.id)}
                      onToggle={() => toggle(registration.id)}
                      suggestion={suggestions.get(registration.id)}
                      switching={transfer.isPending}
                      onSwitch={() => void switchCategories([registration.id])}
                    />
                  ))}
                </ul>
              )}
            </section>

            <section className="space-y-3 rounded-lg bg-white p-6 shadow-sm">
              <h3 className="font-display text-base font-semibold">{t('registrations.add')}</h3>
              {full ? (
                <p className="text-sm text-red-700">{t('registrations.full')}</p>
              ) : (
                <RegistrationForm key={event.id} tournamentId={tournamentId} event={event} />
              )}
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
