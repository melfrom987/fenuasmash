import { useState } from 'react';
import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { Match, MatchStatus } from '@/api/models';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useEvents } from '@/features/events/api';
import {
  useAssignCourt,
  useChangeMatchStatus,
  useCourtAssignments,
  useCreateMatch,
  useMatchQueue,
  useTournamentMatches,
} from '@/features/matches/api';
import { CourtCard } from '@/features/matches/components/CourtCard';
import { CreateMatchForm } from '@/features/matches/components/CreateMatchForm';
import { MatchList } from '@/features/matches/components/MatchList';
import { MatchQueue } from '@/features/matches/components/MatchQueue';
import type { ResultInput } from '@/features/matches/components/ResultForm';
import { LIVE_REFRESH_MS } from '@/features/matches/status';
import { useMatchLabels } from '@/features/matches/use-match-labels';
import { useTournamentRegistrations } from '@/features/registrations/api';
import { useErrorMessage } from '@/lib/use-error-message';
import { useNow } from '@/lib/use-now';

export function MatchesPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const now = useNow(30_000);
  const [selected, setSelected] = useState<Match | null>(null);

  const labels = useMatchLabels(tournamentId);
  const events = useEvents(tournamentId);
  const registrations = useTournamentRegistrations(tournamentId);
  const assignments = useCourtAssignments(tournamentId);
  const queue = useMatchQueue(tournamentId);
  const matches = useTournamentMatches(tournamentId);
  const assign = useAssignCourt(tournamentId);
  const changeStatus = useChangeMatchStatus(tournamentId);
  const createMatch = useCreateMatch(tournamentId);

  const pending = assign.isPending || changeStatus.isPending;
  const actionError = assign.error ?? changeStatus.error;

  const assignTo = (courtId: string) => (match: Match) => {
    changeStatus.reset();
    assign.mutate({ match, courtId }, { onSettled: () => setSelected(null) });
  };

  const setStatus = (match: Match, status: MatchStatus, result?: ResultInput) => {
    assign.reset();
    changeStatus.mutate({
      match,
      status,
      winner: result?.winner ?? null,
      outcome: result?.outcome ?? null,
    });
  };

  if (assignments.isPending || queue.isPending || matches.isPending) {
    return <p className="text-sable-600">{t('common.loading')}</p>;
  }
  if (assignments.isError) return <ErrorMessage>{errorMessage(assignments.error)}</ErrorMessage>;
  if (queue.isError) return <ErrorMessage>{errorMessage(queue.error)}</ErrorMessage>;
  if (matches.isError) return <ErrorMessage>{errorMessage(matches.error)}</ErrorMessage>;

  const nextMatch = queue.data.find((q) => q.available)?.match ?? null;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h2 className="font-display text-lg font-semibold">{t('matches.title')}</h2>
          <p className="text-sable-600 text-sm">{t('matches.hint')}</p>
        </div>
        <p className="text-sable-500 text-xs">
          {t('matches.liveHint', { seconds: LIVE_REFRESH_MS / 1000 })}
        </p>
      </div>

      {actionError && <ErrorMessage>{errorMessage(actionError)}</ErrorMessage>}

      {selected && (
        <div className="bg-lagon-50 border-lagon-200 flex flex-wrap items-center justify-between gap-2 rounded-md border px-4 py-2 text-sm">
          <span>
            {t('matches.pickCourt', {
              sides: `${labels.side(selected, 'A')?.name} ${t('matches.versus')} ${labels.side(selected, 'B')?.name}`,
            })}
          </span>
          <button
            type="button"
            onClick={() => setSelected(null)}
            className="text-lagon-700 hover:underline"
          >
            {t('common.cancel')}
          </button>
        </div>
      )}

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_24rem]">
        <section className="space-y-3">
          <h3 className="font-display text-base font-semibold">{t('matches.courtsTitle')}</h3>
          {assignments.data.length === 0 ? (
            <p className="text-sable-600 rounded-lg bg-white p-6 text-center text-sm shadow-sm">
              {t('tablets.noCourts')}
            </p>
          ) : (
            <ul className="grid gap-3 sm:grid-cols-2 2xl:grid-cols-3">
              {assignments.data.map((assignment) => (
                <CourtCard
                  key={assignment.court.id}
                  assignment={assignment}
                  labels={labels}
                  now={now}
                  selected={selected}
                  nextMatch={nextMatch}
                  pending={pending}
                  onAssign={assignTo(assignment.court.id)}
                  onStatus={setStatus}
                  onMove={setSelected}
                />
              ))}
            </ul>
          )}
        </section>

        <section className="space-y-3">
          <h3 className="font-display text-base font-semibold">
            {t('matches.queueTitle', { count: queue.data.length })}
          </h3>
          <MatchQueue
            queue={queue.data}
            labels={labels}
            selectedId={selected?.id ?? null}
            pending={pending}
            onSelect={setSelected}
            onStatus={setStatus}
          />
        </section>
      </div>

      {events.data && registrations.data && events.data.length > 0 && (
        <CreateMatchForm
          events={events.data}
          registrations={registrations.data}
          pending={createMatch.isPending}
          error={createMatch.error ? errorMessage(createMatch.error) : null}
          onSubmit={(input) => createMatch.mutate(input)}
        />
      )}

      <MatchList
        matches={matches.data}
        events={events.data ?? []}
        labels={labels}
        pending={pending}
        onStatus={setStatus}
      />
    </div>
  );
}
