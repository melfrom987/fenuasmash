import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useClubs } from '@/features/clubs/api';
import { useEvents } from '@/features/events/api';
import { DISCIPLINES } from '@/features/events/constants';
import { seasonReferenceYear } from '@/features/players/age-category';
import { useTournament } from '@/features/tournaments/api';
import { useImportRegistrations } from '@/features/registration-import/api';
import { buildImportRows, type RowFixes } from '@/features/registration-import/build-rows';
import { ColumnsStep } from '@/features/registration-import/components/ColumnsStep';
import { DoneStep } from '@/features/registration-import/components/DoneStep';
import { FileStep } from '@/features/registration-import/components/FileStep';
import { PreviewStep } from '@/features/registration-import/components/PreviewStep';
import { Stepper } from '@/features/registration-import/components/Stepper';
import { partnerKey, type Decisions } from '@/features/registration-import/decisions';
import { type Step } from '@/features/registration-import/steps';
import { ValuesStep } from '@/features/registration-import/components/ValuesStep';
import { suggestColumnMapping, suggestValueMapping } from '@/features/registration-import/mapping';
import { mergeValueMapping } from '@/features/registration-import/merge-values';
import type {
  ColumnMapping,
  ImportReport,
  ImportRow,
  Table,
  ValueMapping,
} from '@/features/registration-import/types';
import { useErrorMessage } from '@/lib/use-error-message';
import {
  applyMoves,
  recordMoves,
  type EntryMoves,
} from '@/features/registration-import/entry-moves';

const noDecisions: Decisions = { players: {}, partners: {} };

export function ImportRegistrationsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const events = useEvents(tournamentId);
  const tournament = useTournament(tournamentId);
  const clubs = useClubs();
  const importRegistrations = useImportRegistrations(tournamentId);

  const [step, setStep] = useState<Step>('file');
  const [table, setTable] = useState<Table | null>(null);
  const [mapping, setMapping] = useState<ColumnMapping | null>(null);
  const [values, setValues] = useState<ValueMapping | null>(null);
  const [fixes, setFixes] = useState<RowFixes>({});
  const [moves, setMoves] = useState<EntryMoves>({});
  const [decisions, setDecisions] = useState<Decisions>(noDecisions);
  const [report, setReport] = useState<ImportReport | null>(null);
  const [stale, setStale] = useState(false);

  const built = useMemo(
    () =>
      table && mapping && values && events.data && clubs.data
        ? buildImportRows({
            table,
            mapping,
            values,
            events: events.data,
            clubs: clubs.data,
            fixes,
          })
        : null,
    [table, mapping, values, events.data, clubs.data, fixes],
  );

  const rows = useMemo(() => (built ? applyMoves(built.rows, moves) : []), [built, moves]);

  const withDecisions = (rows: ImportRow[]): ImportRow[] =>
    rows.map((row) => ({
      ...row,
      playerDecision: decisions.players[row.rowNumber] ?? row.playerDecision,
      entries: row.entries.map((entry) => ({
        ...entry,
        partnerDecision: decisions.partners[partnerKey(row.rowNumber, entry.eventId)],
      })),
    }));

  const send = (dryRun: boolean, source: ImportRow[] = rows) => {
    if (source.length === 0) return;
    importRegistrations.mutate(
      { rows: withDecisions(source), dryRun },
      {
        onSuccess: (result) => {
          setReport(result);
          setStale(false);
          setStep(dryRun ? 'preview' : 'done');
        },
      },
    );
  };

  const restart = () => {
    setStep('file');
    setTable(null);
    setMapping(null);
    setValues(null);
    setFixes({});
    setMoves({});
    setDecisions(noDecisions);
    setReport(null);
    importRegistrations.reset();
  };

  const loadError = events.error ?? clubs.error;
  const error = importRegistrations.error ? errorMessage(importRegistrations.error) : null;
  const noReadableRow = built !== null && built.rows.length === 0;

  return (
    <div className="max-w-6xl space-y-6">
      <div className="space-y-2">
        <Link to=".." relative="path" className="text-lagon-700 text-sm hover:underline">
          {t('registrationImport.backToRegistrations')}
        </Link>
        <h2 className="font-display text-lg font-semibold">{t('registrationImport.title')}</h2>
        <Stepper current={step} />
      </div>

      {loadError && <ErrorMessage>{errorMessage(loadError)}</ErrorMessage>}
      {events.data?.length === 0 && <p className="text-sable-600">{t('registrations.noEvents')}</p>}

      {step === 'file' && (
        <FileStep
          onLoaded={(loaded) => {
            setTable(loaded);
            setMapping(suggestColumnMapping(loaded));
            setValues(null);
            setFixes({});
            setMoves({});
            setDecisions(noDecisions);
            setStep('columns');
          }}
        />
      )}

      {step === 'columns' && table && mapping && (
        <ColumnsStep
          table={table}
          mapping={mapping}
          onChange={setMapping}
          onBack={restart}
          onNext={() => {
            setValues(
              mergeValueMapping(suggestValueMapping(table, mapping, events.data ?? []), values),
            );
            setStep('values');
          }}
        />
      )}

      {step === 'values' && table && mapping && values && (
        <>
          {error && <ErrorMessage>{error}</ErrorMessage>}
          {noReadableRow && (
            <ErrorMessage>{t('registrationImport.values.noReadableRow')}</ErrorMessage>
          )}
          <ValuesStep
            tournamentId={tournamentId}
            offeredDisciplines={tournament.data?.disciplines ?? DISCIPLINES}
            seasonYear={seasonReferenceYear(
              tournament.data ? new Date(tournament.data.startsAt) : new Date(),
            )}
            table={table}
            mapping={mapping}
            values={values}
            events={events.data ?? []}
            clubs={clubs.data ?? []}
            pending={importRegistrations.isPending}
            onChange={setValues}
            onBack={() => setStep('columns')}
            onNext={() => send(true)}
          />
        </>
      )}

      {step === 'preview' && report && built && (
        <PreviewStep
          report={report}
          rows={rows}
          problems={built.problems}
          events={events.data ?? []}
          decisions={decisions}
          stale={stale}
          pending={importRegistrations.isPending}
          error={error}
          onDecide={(next) => setDecisions(next)}
          onSwapName={(row) => {
            setFixes({
              ...fixes,
              [row.rowNumber]: {
                ...fixes[row.rowNumber],
                firstName: row.lastName,
                lastName: row.firstName,
              },
            });
            setStale(true);
          }}
          onFix={(rowNumber, fix) => {
            setFixes({ ...fixes, [rowNumber]: fix });
            setStale(true);
          }}
          onRefresh={() => send(true)}
          onImport={() => send(false)}
          onBack={() => setStep('values')}
          onMoveEntries={(list) => {
            if (!built) return;
            const next = recordMoves(built.rows, moves, list);
            setMoves(next);
            send(true, applyMoves(built.rows, next));
          }}
        />
      )}

      {step === 'done' && report && <DoneStep report={report} onRestart={restart} />}
    </div>
  );
}
