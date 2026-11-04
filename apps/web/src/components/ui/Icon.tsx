import type { ComponentProps } from 'react';
import { HugeiconsIcon } from '@hugeicons/react';

type IconProps = {
  icon: ComponentProps<typeof HugeiconsIcon>['icon'];
  /** Texte lu par les lecteurs d'écran. Sans label, l'icône est décorative. */
  label?: string;
  size?: number;
  className?: string;
};

export function Icon({ icon, label, size = 20, className = '' }: IconProps) {
  return (
    <span
      className={`inline-flex shrink-0 ${className}`}
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
    >
      <HugeiconsIcon icon={icon} size={size} strokeWidth={1.75} color="currentColor" />
    </span>
  );
}
