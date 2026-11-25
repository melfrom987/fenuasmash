import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { Match, MatchSide } from '@/api/models';
import { Button } from '@/components/ui/Button';
import type { MatchLabels } from '../labels';

export type ResultInput = { winner: MatchSide; outcome: 'NORMAL' | 'RETIRED' | null };

export function ResultForm({
  match,
  labels,
  walkover,
  pending,
  onSubmit,
  onCancel,
}: {
  match: Match;
  labels: MatchLabels;
  walkover: boolean;
  pending: boolean;
  onSubmit: (result: ResultInput) => void;
  onCancel: () => void;
}) {
  const { t } = useTranslation();
  const [winner, setWinner] = useState<MatchSide | null>(null);
  const [retired, setRetired] = useState(false);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!winner) return;
    onSubmit({ winner, outcome: walkover ? null : retired ? 'RETIRED' : 'NORMAL' });
  };

  return (
    <form
      onSubmit={submit}
      className="border-sable-200 bg-sable-50 space-y-2 rounded-md border p-3"
    >
      <p className="text-sm font-medium">
        {walkover ? t('matches.result.walkoverWinner') : t('matches.result.winner')}
      </p>
      {(['A', 'B'] as const).map((side) => (
        <label key={side} className="flex items-center gap-2 text-sm">
          <input
            type="radio"
            name={`winner-${match.id}`}
            checked={winner === side}
            onChange={() => setWinner(side)}
          />
          {labels.entry(side === 'A' ? match.sideA : match.sideB) ?? t('matches.unknownSide')}
        </label>
      ))}
      {!walkover && (
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={retired} onChange={(e) => setRetired(e.target.checked)} />
          {t('matches.result.retired')}
        </label>
      )}
      <div className="flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="text-sable-600 text-sm hover:underline">
          {t('common.cancel')}
        </button>
        <Button type="submit" disabled={!winner || pending}>
          {walkover ? t('matches.result.confirmWalkover') : t('matches.result.confirmFinish')}
        </Button>
      </div>
    </form>
  );
}
