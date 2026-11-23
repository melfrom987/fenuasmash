import type { DrawEntry } from '@/api/models';
import { playerName } from '@/features/registrations/format';

export function SeedBadge({ seed }: { seed: number }) {
  return (
    <span className="bg-lagon-600 inline-flex size-5 shrink-0 items-center justify-center rounded-full text-[10px] font-semibold text-white tabular-nums">
      {seed}
    </span>
  );
}

export function EntryLabel({ entry, compact = false }: { entry: DrawEntry; compact?: boolean }) {
  const clubs = [entry.player.clubName, entry.partner?.clubName]
    .filter((c, i, list) => c && list.indexOf(c) === i)
    .join(' / ');
  return (
    <span className="flex min-w-0 items-center gap-2">
      {entry.seed !== null && <SeedBadge seed={entry.seed} />}
      <span className="min-w-0">
        <span className="block truncate font-medium">
          {playerName(entry.player)}
          {entry.partner && ` / ${playerName(entry.partner)}`}
        </span>
        {!compact && <span className="text-sable-600 block truncate text-xs">{clubs}</span>}
      </span>
    </span>
  );
}
