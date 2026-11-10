import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useDeleteEvent, useEvent, useUpdateEvent } from '@/features/events/api';
import { EventForm } from '@/features/events/components/EventForm';
import { toEventFormValues, toEventInput, type EventFormValues } from '@/features/events/schema';
import { useErrorMessage } from '@/lib/use-error-message';

export function EditEventPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const navigate = useNavigate();
  const { tournamentId = '', eventId = '' } = useParams();
  const event = useEvent(eventId);
  const updateEvent = useUpdateEvent(tournamentId, eventId);
  const deleteEvent = useDeleteEvent(tournamentId);
  const [confirming, setConfirming] = useState(false);

  const backToList = () => navigate('..', { relative: 'path' });

  const handleSubmit = async (values: EventFormValues) => {
    await updateEvent.mutateAsync(toEventInput(values));
    await backToList();
  };

  return (
    <div className="max-w-2xl space-y-4">
      <Link to=".." relative="path" className="text-lagon-700 text-sm hover:underline">
        {t('events.backToList')}
      </Link>
      <h2 className="font-display text-lg font-semibold">{t('events.editTitle')}</h2>

      {event.isPending && <p className="text-sable-600">{t('common.loading')}</p>}
      {event.isError && <ErrorMessage>{errorMessage(event.error)}</ErrorMessage>}

      {event.data && (
        <>
          <div className="rounded-lg bg-white p-6 shadow-sm">
            {/* key : réinitialise le formulaire si on passe d'un tableau à un autre */}
            <EventForm
              key={event.data.id}
              defaultValues={toEventFormValues(event.data)}
              onSubmit={handleSubmit}
            />
          </div>

          <section className="space-y-3 rounded-lg border border-red-200 bg-white p-6">
            <div>
              <h3 className="font-medium text-red-800">{t('events.delete.title')}</h3>
              <p className="text-sable-600 text-sm">{t('events.delete.hint')}</p>
            </div>
            {deleteEvent.isError && <ErrorMessage>{errorMessage(deleteEvent.error)}</ErrorMessage>}
            {confirming ? (
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={deleteEvent.isPending}
                  onClick={() => deleteEvent.mutate(eventId, { onSuccess: () => backToList() })}
                  className="rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800 disabled:opacity-50"
                >
                  {t('events.delete.confirm')}
                </button>
                <button
                  type="button"
                  onClick={() => setConfirming(false)}
                  className="border-sable-300 hover:bg-sable-50 rounded-md border bg-white px-4 py-2 text-sm"
                >
                  {t('common.cancel')}
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setConfirming(true)}
                className="rounded-md border border-red-300 bg-white px-4 py-2 text-sm text-red-700 hover:bg-red-50"
              >
                {t('events.delete.button')}
              </button>
            )}
          </section>
        </>
      )}
    </div>
  );
}
