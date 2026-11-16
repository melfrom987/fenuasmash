import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { readTable } from '../parse-file';
import type { Table } from '../types';

export function FileStep({ onLoaded }: { onLoaded: (table: Table) => void }) {
  const { t } = useTranslation();
  const inputId = useId();
  const [reading, setReading] = useState(false);
  const [failed, setFailed] = useState(false);

  const handleFile = async (file: File | undefined) => {
    if (!file) return;
    setReading(true);
    setFailed(false);
    try {
      onLoaded(await readTable(file));
    } catch {
      setFailed(true);
    } finally {
      setReading(false);
    }
  };

  return (
    <section className="space-y-4 rounded-lg bg-white p-6 shadow-sm">
      <div>
        <h2 className="font-display text-base font-semibold">
          {t('registrationImport.file.title')}
        </h2>
        <p className="text-sable-600 mt-1 text-sm">{t('registrationImport.file.hint')}</p>
      </div>
      {failed && <ErrorMessage>{t('registrationImport.file.unreadable')}</ErrorMessage>}
      <label
        htmlFor={inputId}
        className="border-sable-300 hover:border-lagon-400 hover:bg-lagon-50 flex cursor-pointer flex-col items-center gap-1 rounded-lg border-2 border-dashed p-10 text-center"
      >
        <span className="text-lagon-700 font-medium">
          {reading ? t('registrationImport.file.reading') : t('registrationImport.file.choose')}
        </span>
        <span className="text-sable-600 text-xs">{t('registrationImport.file.formats')}</span>
      </label>
      <input
        id={inputId}
        type="file"
        accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        className="sr-only"
        disabled={reading}
        onChange={(e) => {
          void handleFile(e.target.files?.[0]);
          e.target.value = '';
        }}
      />
    </section>
  );
}
