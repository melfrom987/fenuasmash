import { useTranslation } from 'react-i18next';
import { secondaryButtonClass } from './styles';

export function Pagination({
  page,
  size,
  totalItems,
  onChange,
}: {
  /** Page courante, à partir de 0 */
  page: number;
  size: number;
  totalItems: number;
  onChange: (page: number) => void;
}) {
  const { t } = useTranslation();
  const pages = Math.max(1, Math.ceil(totalItems / size));
  if (pages === 1) return null;

  return (
    <nav className="flex items-center justify-between gap-3" aria-label={t('common.pagination')}>
      <button
        type="button"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
        className={secondaryButtonClass}
      >
        {t('common.previous')}
      </button>
      <span className="text-sable-600 text-sm tabular-nums">
        {t('common.pageOf', { page: page + 1, pages })}
      </span>
      <button
        type="button"
        disabled={page >= pages - 1}
        onClick={() => onChange(page + 1)}
        className={secondaryButtonClass}
      >
        {t('common.next')}
      </button>
    </nav>
  );
}
