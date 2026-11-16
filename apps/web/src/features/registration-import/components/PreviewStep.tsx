import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Event } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { secondaryButtonClass } from '@/components/ui/styles';
import { StatusBadge } from '@/features/registrations/components/StatusBadge';
import { formatBirthDate } from '@/lib/dates';
import type { RowProblem } from '../build-rows';
import type { ImportReport, ImportRow, ImportRowReport } from '../types';
import { useIssueMessage } from '../use-issue-message';
import { MatchBadge, MatchSelect } from './MatchSelect';
import { partnerKey, type Decisions } from '../decisions';

const needsAttention = (row: ImportRowReport) =>
  row.issues.length > 0 ||
  row.playerMatch.result === 'PROBABLE' ||
  row.playerMatch.result === 'AMBIGUOUS' ||
  row.entries.some((e) => e.issues.length > 0 || e.status === null);

export function PreviewStep({
  report,
  rows,
  problems,
  events,
  decisions,
  stale,
  pending,
  error,
  onDecide,
  onSwapName,
  onRefresh,
  onImport,
  onBack,
}: {
  report: ImportReport;
  rows: ImportRow[];
  problems: RowProblem[];
  events: Event[];
  decisions: Decisions;
  stale: boolean;
  pending: boolean;
  error: string | null;
  onDecide: (decisions: Decisions) => void;
  onSwapName: (row: ImportRow) => void;
  onRefresh: () => void;
  onImport: () => void;
  onBack: () => void;
}) {
  const { t } = useTranslation();
  const issueMessage = useIssueMessage();
  const [onlyToCheck, setOnlyToCheck] = useState(report.rows.some(needsAttention));

  const rowsByNumber = new Map(rows.map((row) => [row.rowNumber, row]));
  const eventName = (eventId: string) => events.find((e) => e.id === eventId)?.name ?? eventId;
  const shown = onlyToCheck ? report.rows.filter(needsAttention) : report.rows;
  const importable = report.rows.filter((r) => r.issues.length === 0).length;

  const counters = [
    ['rows', report.summary.rows],
    ['playersMatched', report.summary.playersMatched],
    ['playersToCreate', report.summary.playersToCreate],
    ['playersToConfirm', report.summary.playersToConfirm],
    ['confirmed', report.summary.confirmed],
    ['pendingPartner', report.summary.pendingPartner],
    ['lookingForPartner', report.summary.lookingForPartner],
    ['rowsWithIssues', report.summary.rowsWithIssues],
  ] as const;

  return (
    <div className="space-y-6">
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {counters.map(([name, value]) => (
          <div key={name} className="rounded-lg bg-white p-4 shadow-sm">
            <dt className="text-sable-600 text-xs">{t(`registrationImport.summary.${name}`)}</dt>
            <dd className="font-display text-2xl font-semibold tabular-nums">{value}</dd>
          </div>
        ))}
      </dl>

      {problems.length > 0 && (
        <section className="space-y-2 rounded-lg border border-red-200 bg-white p-4">
          <h2 className="font-medium text-red-800">
            {t('registrationImport.preview.unreadableTitle', { count: problems.length })}
          </h2>
          <ul className="space-y-1 text-sm">
            {problems.map((problem) => (
              <li key={problem.rowNumber}>
                {t('registrationImport.preview.line', { n: problem.rowNumber })} —{' '}
                {t(`registrationImport.problems.${problem.code}`, { value: problem.value })}
              </li>
            ))}
          </ul>
        </section>
      )}

      {stale && (
        <p className="bg-sunset-50 text-sunset-800 rounded-md p-3 text-sm">
          {t('registrationImport.preview.stale')}
        </p>
      )}
      {error && <ErrorMessage>{error}</ErrorMessage>}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <label className="text-sable-800 flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={onlyToCheck}
            onChange={(e) => setOnlyToCheck(e.target.checked)}
            className="accent-lagon-600 size-4"
          />
          {t('registrationImport.preview.onlyToCheck')}
        </label>
        <button
          type="button"
          onClick={onRefresh}
          disabled={pending}
          className={secondaryButtonClass}
        >
          {t('registrationImport.preview.refresh')}
        </button>
      </div>

      <div className="overflow-x-auto rounded-lg bg-white shadow-sm">
        <table className="divide-sable-200 min-w-full divide-y text-sm">
          <thead>
            <tr className="text-sable-600 text-left text-xs tracking-wide uppercase">
              <th className="px-4 py-2 font-medium">{t('registrationImport.preview.row')}</th>
              <th className="px-4 py-2 font-medium">{t('registrationImport.preview.player')}</th>
              <th className="px-4 py-2 font-medium">{t('registrationImport.preview.match')}</th>
              <th className="px-4 py-2 font-medium">{t('registrationImport.preview.entries')}</th>
            </tr>
          </thead>
          <tbody className="divide-sable-200 divide-y align-top">
            {shown.length === 0 && (
              <tr>
                <td colSpan={4} className="text-sable-600 px-4 py-6 text-center">
                  {t('registrationImport.preview.nothingToCheck')}
                </td>
              </tr>
            )}
            {shown.map((rowReport) => {
              const row = rowsByNumber.get(rowReport.rowNumber);
              if (!row) return null;
              return (
                <tr key={row.rowNumber}>
                  <td className="text-sable-600 px-4 py-3 tabular-nums">{row.rowNumber}</td>
                  <td className="px-4 py-3">
                    <p className="font-medium">
                      <span className="uppercase">{row.lastName}</span> {row.firstName}
                    </p>
                    <p className="text-sable-600 text-xs">
                      {formatBirthDate(row.birthDate)} · {row.clubName}
                    </p>
                    <button
                      type="button"
                      onClick={() => onSwapName(row)}
                      className="text-lagon-700 text-xs hover:underline"
                    >
                      {t('registrationImport.preview.swapName')}
                    </button>
                  </td>
                  <td className="min-w-56 px-4 py-3">
                    <MatchBadge result={rowReport.playerMatch.result} />
                    <MatchSelect
                      match={rowReport.playerMatch}
                      decision={decisions.players[row.rowNumber]}
                      allowNew
                      onChange={(decision) => {
                        const players = { ...decisions.players };
                        if (decision) players[row.rowNumber] = decision;
                        else delete players[row.rowNumber];
                        onDecide({ ...decisions, players });
                      }}
                    />
                    {rowReport.issues.map((issue) => (
                      <p key={issue.code} className="mt-1 text-xs text-red-700">
                        {issueMessage(issue)}
                      </p>
                    ))}
                  </td>
                  <td className="px-4 py-3">
                    {rowReport.entries.length === 0 && (
                      <p className="text-sable-600 text-xs">
                        {t('registrationImport.preview.noEntry')}
                      </p>
                    )}
                    <ul className="space-y-2">
                      {rowReport.entries.map((entry) => {
                        const sent = row.entries.find((e) => e.eventId === entry.eventId);
                        const key = partnerKey(row.rowNumber, entry.eventId);
                        return (
                          <li key={entry.eventId}>
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="font-medium">{eventName(entry.eventId)}</span>
                              {entry.status ? (
                                <StatusBadge status={entry.status} />
                              ) : (
                                <span className="text-xs font-medium text-red-700">
                                  {t('registrationImport.preview.notImported')}
                                </span>
                              )}
                            </div>
                            {sent?.partner.kind === 'NAMED' && (
                              <div className="text-sable-700 mt-1 text-xs">
                                {t('registrationImport.preview.partner', {
                                  name: sent.partner.name,
                                })}{' '}
                                {entry.partnerMatch && (
                                  <MatchBadge result={entry.partnerMatch.result} />
                                )}
                                {entry.partnerMatch && (
                                  <MatchSelect
                                    match={entry.partnerMatch}
                                    decision={decisions.partners[key]}
                                    allowNew={false}
                                    onChange={(decision) => {
                                      const partners = { ...decisions.partners };
                                      if (decision) partners[key] = decision;
                                      else delete partners[key];
                                      onDecide({ ...decisions, partners });
                                    }}
                                  />
                                )}
                              </div>
                            )}
                            {entry.issues.map((issue) => (
                              <p key={issue.code} className="mt-1 text-xs text-red-700">
                                {issueMessage(issue)}
                              </p>
                            ))}
                          </li>
                        );
                      })}
                    </ul>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <button type="button" onClick={onBack} className={secondaryButtonClass}>
          {t('registrationImport.back')}
        </button>
        <Button type="button" disabled={pending || importable === 0} onClick={onImport}>
          {pending
            ? t('registrationImport.preview.importing')
            : t('registrationImport.preview.import', { count: importable })}
        </Button>
      </div>
    </div>
  );
}
