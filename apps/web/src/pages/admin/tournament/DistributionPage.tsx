import { useParams } from 'react-router';
import { useQueries } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { drawQuery } from '@/features/draw/api';
import { DistributionCard } from '@/features/draw/components/DistributionCard';
import { orderedEvents } from '@/features/draw/ladder';
import { useEvents } from '@/features/events/api';
import { registrationLabel } from '@/features/matches/labels';
import { useTournamentRegistrations, useTransferRegistrations } from '@/features/registrations/api';
import { ApiError } from '@/lib/api-client';
import { useErrorMessage } from '@/lib/use-error-message';

export function DistributionPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const events = useEvents(tournamentId);
  const registrations = useTournamentRegistrations(tournamentId);
  const transfer = useTransferRegistrations(tournamentId);
  const draws = useQueries({
    queries: (events.data ?? []).map((event) => drawQuery(event.id)),
  });

  if (events.isPending || registrations.isPending) {
    return <p className="text-sable-600">{t('common.loading')}</p>;
  }
  if (events.isError) return <ErrorMessage>{errorMessage(events.error)}</ErrorMessage>;
  if (registrations.isError)
    return <ErrorMessage>{errorMessage(registrations.error)}</ErrorMessage>;

  const published = new Set(
    events.data.filter((_, i) => draws[i]?.data?.status === 'PUBLISHED').map((e) => e.id),
  );
  const parents = new Set(events.data.map((e) => e.parentEventId));
  const rejected = transfer.data?.rejected ?? [];
  const nameOf = (id: string) => {
    const registration = registrations.data.find((r) => r.id === id);
    return registration ? registrationLabel(registration) : id;
  };

  return (
    <div className="space-y-4">
      <div>
        <h2 className="font-display text-lg font-semibold">{t('draws.distribution.title')}</h2>
        <p className="text-sable-600 text-sm">{t('draws.distribution.hint')}</p>
      </div>

      {transfer.isError && <ErrorMessage>{errorMessage(transfer.error)}</ErrorMessage>}
      {rejected.length > 0 && (
        <ErrorMessage>
          {rejected
            .map(
              (r) =>
                `${nameOf(r.registrationId)} : ${errorMessage(new ApiError(400, r.message, r.code))}`,
            )
            .join(' ')}
        </ErrorMessage>
      )}

      <ul className="grid items-start gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {orderedEvents(events.data).map((event) => (
          <DistributionCard
            key={event.id}
            event={event}
            events={events.data}
            registrations={registrations.data.filter((r) => r.eventId === event.id)}
            locked={published.has(event.id)}
            split={parents.has(event.id)}
            pending={transfer.isPending}
            onMove={(registrationIds, targetEventId) =>
              transfer.mutate({ registrationIds, targetEventId })
            }
          />
        ))}
      </ul>
    </div>
  );
}
