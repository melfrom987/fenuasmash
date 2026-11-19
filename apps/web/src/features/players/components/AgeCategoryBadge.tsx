import { useTranslation } from 'react-i18next';
import { getAgeCategory, seasonReferenceYear } from '../age-category';

export function AgeCategoryBadge({
  birthDate,
  referenceYear = seasonReferenceYear(),
}: {
  birthDate: string;
  referenceYear?: number;
}) {
  const { t } = useTranslation();
  const category = getAgeCategory(birthDate, referenceYear);
  const isYouth = category.code !== 'SENIOR' && category.code !== 'VETERAN';

  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium whitespace-nowrap ${
        isYouth ? 'bg-lagon-100 text-lagon-800' : 'bg-sable-100 text-sable-800'
      }`}
    >
      {category.code === 'VETERAN'
        ? t('players.ageCategories.VETERAN', { level: category.level })
        : t(`players.ageCategories.${category.code}`)}
    </span>
  );
}
