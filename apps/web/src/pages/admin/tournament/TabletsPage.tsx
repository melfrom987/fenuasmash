import { useState } from 'react';
import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import type { PairingCode } from '@/api/models';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { useCourts } from '@/features/courts/api';
import {
  useCreatePairingCode,
  useRevokeTablet,
  useSimulatePairing,
  useTablets,
} from '@/features/tablets/api';
import { CourtTabletCard } from '@/features/tablets/components/CourtTabletCard';
import { TabletHistory } from '@/features/tablets/components/TabletHistory';
import { pairedTabletOf, POLL_MS, POLL_WHILE_PAIRING_MS } from '@/features/tablets/status';
import { useErrorMessage } from '@/lib/use-error-message';
import { useNow } from '@/lib/use-now';

const CODE_LIFETIME_MS = 10 * 60 * 1000;
const isMock = import.meta.env.MODE === 'mock';

const issuedAt = (code: PairingCode) => Date.parse(code.expiresAt) - CODE_LIFETIME_MS;

export function TabletsPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const { tournamentId = '' } = useParams();
  const now = useNow(POLL_MS);
  const [codes, setCodes] = useState<Record<string, PairingCode>>({});

  const courts = useCourts(tournamentId);
  const createCode = useCreatePairingCode();
  const revoke = useRevokeTablet(tournamentId);
  const simulate = useSimulatePairing(tournamentId);

  const waiting = Object.values(codes).some((code) => Date.parse(code.expiresAt) > now);
  const tablets = useTablets(tournamentId, waiting ? POLL_WHILE_PAIRING_MS : POLL_MS);
  const allTablets = tablets.data ?? [];

  const requestCode = (courtId: string) =>
    createCode.mutate(courtId, {
      onSuccess: (code) => setCodes((current) => ({ ...current, [courtId]: code })),
    });

  if (courts.isPending || tablets.isPending) {
    return <p className="text-sable-600">{t('common.loading')}</p>;
  }
  if (courts.isError) return <ErrorMessage>{errorMessage(courts.error)}</ErrorMessage>;
  if (tablets.isError) return <ErrorMessage>{errorMessage(tablets.error)}</ErrorMessage>;

  const revoked = allTablets.filter((tablet) => tablet.revokedAt !== null);

  return (
    <div className="max-w-4xl space-y-4">
      <div>
        <h2 className="font-display text-lg font-semibold">{t('tablets.title')}</h2>
        <p className="text-sable-600 text-sm">{t('tablets.hint')}</p>
      </div>

      {courts.data.length === 0 ? (
        <p className="text-sable-600 rounded-lg bg-white p-6 text-center shadow-sm">
          {t('tablets.noCourts')}
        </p>
      ) : (
        <ul className="space-y-3">
          {courts.data.map((court) => {
            const tablet = pairedTabletOf(allTablets, court.id);
            const code = codes[court.id];
            const justPaired =
              code !== undefined &&
              tablet !== undefined &&
              Date.parse(tablet.pairedAt) >= issuedAt(code);
            const shownCode = code && !justPaired ? code : undefined;

            return (
              <CourtTabletCard
                key={court.id}
                court={court}
                tablet={tablet}
                pairingCode={shownCode}
                justPaired={justPaired}
                now={now}
                creating={createCode.isPending && createCode.variables === court.id}
                revoking={revoke.isPending && revoke.variables === tablet?.id}
                error={
                  createCode.error && createCode.variables === court.id
                    ? errorMessage(createCode.error)
                    : revoke.error && tablet && revoke.variables === tablet.id
                      ? errorMessage(revoke.error)
                      : null
                }
                onCreateCode={() => requestCode(court.id)}
                onRevoke={() => tablet && revoke.mutate(tablet.id)}
                simulating={simulate.isPending}
                onSimulate={isMock && shownCode ? () => simulate.mutate(shownCode.code) : undefined}
              />
            );
          })}
        </ul>
      )}

      <TabletHistory tablets={revoked} />
    </div>
  );
}
