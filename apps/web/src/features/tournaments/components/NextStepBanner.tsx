import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { NextAction } from '../progress';

export function NextStepBanner({ next, here }: { next: NextAction; here: boolean }) {
  const { t } = useTranslation();

  const text = (() => {
    switch (next.kind) {
      case 'register':
        return t('tournamentDetail.next.register', { count: next.count });
      case 'runDraw':
      case 'publishDraw':
      case 'moveEntries':
        return t(`tournamentDetail.next.${next.kind}`, { event: next.event });
      default:
        return t(`tournamentDetail.next.${next.kind}`);
    }
  })();

  return (
    <div className="bg-sunset-50 border-sunset-200 flex flex-wrap items-center justify-between gap-2 rounded-md border px-4 py-2 text-sm">
      <p>
        <span className="text-sunset-800 font-medium">{t('tournamentDetail.next.label')}</span>{' '}
        {text}
      </p>
      {!here && (
        <Link to={next.to} className="text-lagon-700 font-medium hover:underline">
          {t('tournamentDetail.next.go')}
        </Link>
      )}
    </div>
  );
}
