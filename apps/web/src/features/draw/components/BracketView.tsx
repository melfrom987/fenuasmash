import { useTranslation } from 'react-i18next';
import type { Bracket, BracketSlot, DrawSlotRef } from '@/api/models';
import { EntryLabel } from './EntryLabel';

const SLOT_HEIGHT = 36;

export function BracketView({
  bracket,
  editable,
  selected,
  onSelect,
}: {
  bracket: Bracket;
  editable: boolean;
  selected: DrawSlotRef | null;
  onSelect: (ref: DrawSlotRef) => void;
}) {
  const { t } = useTranslation();
  const rounds = Math.log2(bracket.size);
  const firstRoundMatches = bracket.size / 2;
  const height = firstRoundMatches * (SLOT_HEIGHT * 2 + 16);

  const roundName = (matches: number) =>
    matches === 1
      ? t('draws.rounds.final')
      : matches === 2
        ? t('draws.rounds.semi')
        : matches === 4
          ? t('draws.rounds.quarter')
          : t('draws.rounds.roundOf', { count: matches * 2 });

  const isSelected = (slot: BracketSlot) =>
    selected !== null && 'position' in selected && selected.position === slot.position;

  const slotContent = (slot: BracketSlot) => {
    if (slot.bye) return <span className="text-sable-500 italic">{t('draws.bye')}</span>;
    if (slot.entry) return <EntryLabel entry={slot.entry} compact />;
    if (slot.qualifier) {
      return (
        <span className="text-sable-700">
          {t('draws.qualifier', {
            count: slot.qualifier.rank,
            ordinal: true,
            pool: slot.qualifier.pool,
          })}
        </span>
      );
    }
    return null;
  };

  return (
    <div className="overflow-x-auto rounded-lg bg-white p-4 shadow-sm">
      <div className="flex gap-6">
        {Array.from({ length: rounds }, (_, round) => {
          const matches = firstRoundMatches / 2 ** round;
          return (
            <div key={round} className="w-64 shrink-0">
              <p className="text-sable-600 mb-2 text-xs font-medium tracking-wide uppercase">
                {roundName(matches)}
              </p>
              <div className="flex flex-col justify-around" style={{ height }}>
                {Array.from({ length: matches }, (_, m) => {
                  const pair =
                    round === 0 ? [bracket.slots[m * 2], bracket.slots[m * 2 + 1]] : null;
                  return (
                    <div
                      key={m}
                      className="border-sable-200 divide-sable-200 divide-y rounded-md border text-sm"
                    >
                      {[0, 1].map((side) => {
                        const slot = pair?.[side];
                        const clickable = editable && slot !== undefined && !slot.bye;
                        return (
                          <button
                            key={side}
                            type="button"
                            disabled={!clickable}
                            onClick={() => slot && onSelect({ position: slot.position })}
                            style={{ height: SLOT_HEIGHT }}
                            className={`flex w-full items-center gap-2 px-2 text-left ${
                              slot && isSelected(slot)
                                ? 'bg-lagon-100'
                                : clickable
                                  ? 'hover:bg-sable-50'
                                  : ''
                            }`}
                          >
                            {slot && (
                              <span className="text-sable-400 w-5 shrink-0 text-right text-[10px] tabular-nums">
                                {slot.position}
                              </span>
                            )}
                            {slot ? slotContent(slot) : null}
                          </button>
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
