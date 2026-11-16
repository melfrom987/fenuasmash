import { useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { RegistrationStatus } from '@/api/models';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { inputClass } from '@/components/ui/styles';
import { useEvents } from '@/features/events/api';
import { useTournamentRegistrations } from '@/features/registrations/api';
import { EventPicker } from '@/features/registrations/components/EventPicker';
import { RegistrationForm } from '@/features/registrations/components/RegistrationForm';
import { RegistrationRow } from '@/features/registrations/components/RegistrationRow';
import { isDoubles, REGISTRATION_STATUSES } from '@/features/registrations/constants';
import { useErrorMessage } from '@/lib/use-error-message';

export function RegistrationsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const events = useEvents(tournamentId);
  const registrations = useTournamentRegistrations(tournamentId);
  const [status, setStatus] = useState<RegistrationStatus | ''>('');

  const [searchParams, setSearchParams] = useSearchParams();
  const selectedId = searchParams.get('event') ?? events.data?.[0]?.id;
  const event = events.data?.find((e) => e.id === selectedId);

  const counts = useMemo(() => {
    const map = new Map<string, number>();
    for (const r of registrations.data ?? []) map.set(r.eventId, (map.get(r.eventId) ?? 0) + 1);
    return map;
  }, [registrations.data]);

  const eventRegistrations = (registrations.data ?? []).filter(
    (r) => r.eventId === selectedId && (!status || r.status === status),
  );
  const count = event ? (counts.get(event.id) ?? 0) : 0;
  const full = event !== undefined && event.maxEntries !== null && count >= event.maxEntries;

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

            <section className="space-y-3 rounded-lg bg-white p-6 shadow-sm">
              <h3 className="font-display text-base font-semibold">{t('registrations.add')}</h3>
              {full ? (
                <p className="text-sm text-red-700">{t('registrations.full')}</p>
              ) : (
                <RegistrationForm key={event.id} tournamentId={tournamentId} event={event} />
              )}
            </section>

            <section className="space-y-3">
              <div className="flex flex-wrap items-end justify-between gap-3">
                <h3 className="font-display text-base font-semibold">
                  {t('registrations.listTitle')}
                </h3>
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
                  {eventRegistrations.map((registration) => (
                    <RegistrationRow
                      key={registration.id}
                      tournamentId={tournamentId}
                      event={event}
                      registration={registration}
                    />
                  ))}
                </ul>
              )}
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
