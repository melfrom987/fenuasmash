import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { secondaryButtonClass } from '@/components/ui/styles';
import type { ImportReport } from '../types';
import { useIssueMessage } from '../use-issue-message';

export function DoneStep({ report, onRestart }: { report: ImportReport; onRestart: () => void }) {
  const { t } = useTranslation();
  const issueMessage = useIssueMessage();
  const imported = report.rows.filter((r) => r.imported).length;
  const skipped = report.rows.filter((r) => !r.imported);

  return (
    <section className="space-y-4 rounded-lg bg-white p-6 shadow-sm">
      <h2 className="font-display text-lg font-semibold">
        {t('registrationImport.done.title', { count: imported })}
      </h2>
      <p className="text-sable-700 text-sm">
        {t('registrationImport.done.registrations', {
          confirmed: report.summary.confirmed,
          pending: report.summary.pendingPartner,
          looking: report.summary.lookingForPartner,
        })}
      </p>

      {skipped.length > 0 && (
        <div className="space-y-1">
          <h3 className="font-medium text-red-800">
            {t('registrationImport.done.skipped', { count: skipped.length })}
          </h3>
          <ul className="text-sm">
            {skipped.map((row) => (
              <li key={row.rowNumber}>
                {t('registrationImport.preview.line', { n: row.rowNumber })} —{' '}
                {row.issues.map(issueMessage).join(' · ')}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        <Link
          to=".."
          relative="path"
          className="bg-lagon-600 hover:bg-lagon-700 rounded-md px-4 py-2 text-sm font-medium text-white"
        >
          {t('registrationImport.done.seeRegistrations')}
        </Link>
        <button type="button" onClick={onRestart} className={secondaryButtonClass}>
          {t('registrationImport.done.again')}
        </button>
      </div>
    </section>
  );
}
