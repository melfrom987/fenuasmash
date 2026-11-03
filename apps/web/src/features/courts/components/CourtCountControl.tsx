import { useTranslation } from 'react-i18next';
import type { Court } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useErrorMessage } from '@/lib/use-error-message';
import { useAddCourt, useRemoveCourt } from '../api';

export function CourtCountControl({
  tournamentId,
  courts,
}: {
  tournamentId: string;
  courts: Court[];
}) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const addCourt = useAddCourt(tournamentId);
  const removeCourt = useRemoveCourt(tournamentId);

  const last = courts.at(-1);
  const busy = addCourt.isPending || removeCourt.isPending;
  const error = addCourt.error ?? removeCourt.error;

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-3 rounded-lg bg-white p-4 shadow-sm">
        <span className="font-medium">{t('courts.total', { total: courts.length })}</span>
        <div className="ml-auto flex gap-2">
          {last && (
            <button
              type="button"
              disabled={busy}
              onClick={() => removeCourt.mutate(last.id)}
              className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm hover:bg-slate-50 disabled:opacity-50"
            >
              {t('courts.removeLast', { number: last.number })}
            </button>
          )}
          <Button type="button" disabled={busy} onClick={() => addCourt.mutate()}>
            {t('courts.add')}
          </Button>
        </div>
      </div>
      {error && <ErrorMessage>{errorMessage(error)}</ErrorMessage>}
    </div>
  );
}
