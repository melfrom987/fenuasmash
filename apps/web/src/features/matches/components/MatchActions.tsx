import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Match, MatchStatus } from '@/api/models';
import type { MatchLabels } from '../labels';
import { canMove, NEEDS_BOTH_SIDES } from '../status';
import { ResultForm, type ResultInput } from './ResultForm';

type Mode = 'idle' | 'finish' | 'walkover' | 'cancel';

const NEXT = {
  SCHEDULED: 'READY',
  CALLED: 'WARMUP',
  WARMUP: 'IN_PROGRESS',
} as const satisfies Partial<Record<MatchStatus, MatchStatus>>;

const BACK = {
  READY: 'SCHEDULED',
  CALLED: 'READY',
  WARMUP: 'CALLED',
  FINISHED: 'IN_PROGRESS',
} as const satisfies Partial<Record<MatchStatus, MatchStatus>>;

const hasNext = (status: MatchStatus): status is keyof typeof NEXT => status in NEXT;
const hasBack = (status: MatchStatus): status is keyof typeof BACK => status in BACK;

const linkClass = 'text-sm hover:underline disabled:opacity-50';

export function MatchActions({
  match,
  labels,
  pending,
  onStatus,
  onCall,
  onMove,
}: {
  match: Match;
  labels: MatchLabels;
  pending: boolean;
  onStatus: (status: MatchStatus, result?: ResultInput) => void;
  onCall?: () => void;
  onMove?: () => void;
}) {
  const { t } = useTranslation();
  const [mode, setMode] = useState<Mode>('idle');
  const complete = match.sideA !== null && match.sideB !== null;
  const allowed = (status: MatchStatus) =>
    canMove(match.status, status) && (complete || !NEEDS_BOTH_SIDES(status));

  if (mode === 'finish' || mode === 'walkover') {
    return (
      <ResultForm
        match={match}
        labels={labels}
        walkover={mode === 'walkover'}
        pending={pending}
        onCancel={() => setMode('idle')}
        onSubmit={(result) => {
          onStatus(mode === 'walkover' ? 'WALKOVER' : 'FINISHED', result);
          setMode('idle');
        }}
      />
    );
  }

  if (mode === 'cancel') {
    return (
      <div className="flex flex-wrap items-center justify-end gap-2">
        <span className="text-sm">{t('matches.actions.cancelConfirm')}</span>
        <button type="button" onClick={() => setMode('idle')} className={linkClass}>
          {t('common.cancel')}
        </button>
        <button
          type="button"
          disabled={pending}
          onClick={() => {
            onStatus('CANCELLED');
            setMode('idle');
          }}
          className="rounded-md bg-red-600 px-3 py-1 text-sm text-white hover:bg-red-700 disabled:opacity-50"
        >
          {t('matches.actions.cancelMatch')}
        </button>
      </div>
    );
  }

  const status = match.status;
  const back = hasBack(status) ? BACK[status] : null;

  return (
    <div className="flex flex-wrap items-center justify-end gap-x-3 gap-y-1">
      {hasBack(status) && back && allowed(back) && (
        <button
          type="button"
          disabled={pending}
          onClick={() => onStatus(back)}
          className={`text-sable-600 ${linkClass}`}
        >
          {t(`matches.actions.back.${status}`)}
        </button>
      )}
      {allowed('WALKOVER') && (
        <button
          type="button"
          disabled={pending}
          onClick={() => setMode('walkover')}
          className={`text-hibiscus-700 ${linkClass}`}
        >
          {t('matches.actions.walkover')}
        </button>
      )}
      {allowed('CANCELLED') && (
        <button
          type="button"
          disabled={pending}
          onClick={() => setMode('cancel')}
          className={`text-red-700 ${linkClass}`}
        >
          {t('matches.actions.cancel')}
        </button>
      )}
      {onMove && match.status === 'CALLED' && (
        <button
          type="button"
          disabled={pending}
          onClick={onMove}
          className={`text-lagon-700 ${linkClass}`}
        >
          {t('matches.actions.move')}
        </button>
      )}
      {onCall && match.status === 'READY' && (
        <button
          type="button"
          disabled={pending}
          onClick={onCall}
          className="bg-lagon-600 hover:bg-lagon-700 rounded-md px-3 py-1 text-sm font-medium text-white disabled:opacity-50"
        >
          {t('matches.actions.call')}
        </button>
      )}
      {hasNext(status) && (
        <button
          type="button"
          disabled={pending || !allowed(NEXT[status])}
          title={allowed(NEXT[status]) ? undefined : t('matches.sidesMissing')}
          onClick={() => onStatus(NEXT[status])}
          className="bg-lagon-600 hover:bg-lagon-700 rounded-md px-3 py-1 text-sm font-medium text-white disabled:opacity-50"
        >
          {t(`matches.actions.next.${status}`)}
        </button>
      )}
      {match.status === 'IN_PROGRESS' && (
        <button
          type="button"
          disabled={pending}
          onClick={() => setMode('finish')}
          className="bg-lagon-600 hover:bg-lagon-700 rounded-md px-3 py-1 text-sm font-medium text-white disabled:opacity-50"
        >
          {t('matches.actions.finish')}
        </button>
      )}
    </div>
  );
}
