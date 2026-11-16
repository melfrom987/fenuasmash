import type { ClubSummary } from '@/api/models';

const sizes = {
  sm: 'size-6 text-[10px]',
  md: 'size-12 text-sm',
  lg: 'size-20 text-lg',
};

const initials = (name: string) =>
  name
    .split(/[\s'’-]+/)
    .filter((word) => word.length > 2 || /^[A-Z]/.test(word))
    .slice(0, 2)
    .map((word) => word[0].toUpperCase())
    .join('');

export function ClubLogo({
  club,
  size = 'md',
}: {
  club: Pick<ClubSummary, 'name' | 'logoUrl'>;
  size?: keyof typeof sizes;
}) {
  if (club.logoUrl) {
    return (
      <img
        src={club.logoUrl}
        alt=""
        className={`${sizes[size]} shrink-0 rounded-md bg-white object-contain`}
      />
    );
  }
  return (
    <span
      aria-hidden
      className={`${sizes[size]} bg-lagon-100 text-lagon-800 font-display inline-flex shrink-0 items-center justify-center rounded-md font-semibold`}
    >
      {initials(club.name)}
    </span>
  );
}
