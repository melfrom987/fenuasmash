import { useTranslation } from 'react-i18next';
import type { Court } from '@/api/models';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useErrorMessage } from '@/lib/use-error-message';
import { useUpdateCourt } from '../api';

export function CourtList({ tournamentId, courts }: { tournamentId: string; courts: Court[] }) {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const updateCourt = useUpdateCourt(tournamentId);

  return (
    <div className="space-y-2">
      {updateCourt.isError && <ErrorMessage>{errorMessage(updateCourt.error)}</ErrorMessage>}

      <ul className="divide-y divide-slate-200 rounded-lg bg-white shadow-sm">
        {courts.map((court) => {
          const updating = updateCourt.isPending && updateCourt.variables?.courtId === court.id;

          return (
            <li key={court.id} className="flex items-center justify-between gap-3 px-4 py-3">
              <div className="flex items-center gap-3">
                <span className={`font-medium ${court.active ? '' : 'text-slate-400'}`}>
                  {t('courts.label', { number: court.number })}
                </span>
                <span
                  className={`rounded px-1.5 py-0.5 text-xs ${
                    court.active ? 'bg-green-100 text-green-800' : 'bg-slate-200 text-slate-600'
                  }`}
                >
                  {court.active ? t('courts.active') : t('courts.inactive')}
                </span>
              </div>
              <button
                type="button"
                disabled={updating}
                onClick={() => updateCourt.mutate({ courtId: court.id, active: !court.active })}
                className="text-sm text-blue-600 hover:underline disabled:opacity-50"
              >
                {court.active ? t('courts.deactivate') : t('courts.activate')}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
