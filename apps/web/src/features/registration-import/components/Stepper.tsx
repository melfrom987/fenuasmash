import { useTranslation } from 'react-i18next';
import { STEPS, type Step } from '../steps';

export function Stepper({ current }: { current: Step }) {
  const { t } = useTranslation();
  const currentIndex = STEPS.indexOf(current);

  return (
    <ol className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
      {STEPS.map((step, index) => (
        <li
          key={step}
          aria-current={step === current ? 'step' : undefined}
          className={`flex items-center gap-2 ${
            index === currentIndex
              ? 'text-lagon-700 font-semibold'
              : index < currentIndex
                ? 'text-sable-700'
                : 'text-sable-500'
          }`}
        >
          <span
            className={`inline-flex size-6 items-center justify-center rounded-full text-xs tabular-nums ${
              index <= currentIndex ? 'bg-lagon-600 text-white' : 'bg-sable-200'
            }`}
          >
            {index + 1}
          </span>
          {t(`registrationImport.steps.${step}`)}
        </li>
      ))}
    </ol>
  );
}
