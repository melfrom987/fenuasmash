import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { Event, Registration } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { inputClass } from '@/components/ui/styles';
import { registrationLabel } from '../labels';

export function CreateMatchForm({
  events,
  registrations,
  pending,
  error,
  onSubmit,
}: {
  events: Event[];
  registrations: Registration[];
  pending: boolean;
  error: string | null;
  onSubmit: (input: {
    eventId: string;
    round: number | null;
    sideA: string | null;
    sideB: string | null;
  }) => void;
}) {
  const { t } = useTranslation();
  const [eventId, setEventId] = useState(events[0]?.id ?? '');
  const [round, setRound] = useState('');
  const [sideA, setSideA] = useState('');
  const [sideB, setSideB] = useState('');

  const entries = registrations
    .filter((r) => r.eventId === eventId && r.status === 'CONFIRMED')
    .sort((a, b) => registrationLabel(a).localeCompare(registrationLabel(b)));
  const sameSide = sideA !== '' && sideA === sideB;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!eventId || sameSide) return;
    onSubmit({
      eventId,
      round: round === '' ? null : Number(round),
      sideA: sideA || null,
      sideB: sideB || null,
    });
    setSideA('');
    setSideB('');
  };

  const sideSelect = (value: string, onChange: (value: string) => void, label: string) => (
    <label className="block min-w-0">
      <span className="text-sable-800 text-sm font-medium">{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)} className={inputClass}>
        <option value="">{t('matches.create.tbd')}</option>
        {entries.map((r) => (
          <option key={r.id} value={r.id}>
            {registrationLabel(r)}
          </option>
        ))}
      </select>
    </label>
  );

  return (
    <details className="rounded-lg bg-white shadow-sm">
      <summary className="cursor-pointer px-4 py-3 text-sm font-medium">
        {t('matches.create.title')}
      </summary>
      <form onSubmit={submit} className="border-sable-200 space-y-3 border-t p-4">
        <p className="text-sable-600 text-sm">{t('matches.create.hint')}</p>
        {error && <ErrorMessage>{error}</ErrorMessage>}
        <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_6rem]">
          <label className="block min-w-0">
            <span className="text-sable-800 text-sm font-medium">{t('matches.create.event')}</span>
            <select
              value={eventId}
              onChange={(e) => {
                setEventId(e.target.value);
                setSideA('');
                setSideB('');
              }}
              className={inputClass}
            >
              {events.map((event) => (
                <option key={event.id} value={event.id}>
                  {event.name}
                </option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="text-sable-800 text-sm font-medium">{t('matches.create.round')}</span>
            <input
              type="number"
              min={1}
              max={20}
              value={round}
              onChange={(e) => setRound(e.target.value)}
              className={inputClass}
            />
          </label>
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          {sideSelect(sideA, setSideA, t('matches.create.sideA'))}
          {sideSelect(sideB, setSideB, t('matches.create.sideB'))}
        </div>
        {sameSide && <p className="text-sm text-red-700">{t('matches.create.sameSide')}</p>}
        <div className="flex justify-end">
          <Button type="submit" disabled={pending || !eventId || sameSide}>
            {t('matches.create.submit')}
          </Button>
        </div>
      </form>
    </details>
  );
}
