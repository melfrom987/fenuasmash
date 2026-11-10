import { Link, useNavigate, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useCreateEvent } from '@/features/events/api';
import { EventForm } from '@/features/events/components/EventForm';
import { toEventInput, type EventFormValues } from '@/features/events/schema';

export function CreateEventPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { tournamentId = '' } = useParams();
  const createEvent = useCreateEvent(tournamentId);

  const handleSubmit = async (values: EventFormValues) => {
    await createEvent.mutateAsync(toEventInput(values));
    await navigate('..', { relative: 'path' });
  };

  return (
    <div className="max-w-2xl space-y-4">
      <Link to=".." relative="path" className="text-lagon-700 text-sm hover:underline">
        {t('events.backToList')}
      </Link>
      <h2 className="font-display text-lg font-semibold">{t('events.createTitle')}</h2>
      <div className="rounded-lg bg-white p-6 shadow-sm">
        <EventForm onSubmit={handleSubmit} />
      </div>
    </div>
  );
}
