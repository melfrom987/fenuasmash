import { Link, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useEvents } from '@/features/events/api';
import { EventList } from '@/features/events/components/EventList';
import { useErrorMessage } from '@/lib/use-error-message';

export function EventsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const events = useEvents(tournamentId);

  return (
    <div className="max-w-3xl space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-display text-lg font-semibold">{t('events.title')}</h2>
          <p className="text-sable-600 text-sm">{t('events.hint')}</p>
        </div>
        <Link
          to="new"
          className="bg-lagon-600 hover:bg-lagon-700 rounded-md px-4 py-2 text-sm font-medium text-white"
        >
          {t('events.new')}
        </Link>
      </div>

      {events.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
      {events.isError && <ErrorMessage>{errorMessage(events.error)}</ErrorMessage>}
      {events.data?.length === 0 && <p className="text-sable-600">{t('events.empty')}</p>}
      {events.data && events.data.length > 0 && <EventList events={events.data} />}
    </div>
  );
}
