import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import type { ImportReport, ImportRow } from './types';

export function useImportRegistrations(tournamentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ rows, dryRun }: { rows: ImportRow[]; dryRun: boolean }) =>
      api<ImportReport>(`/tournaments/${tournamentId}/registrations/import?dryRun=${dryRun}`, {
        method: 'POST',
        body: JSON.stringify({ rows }),
      }),
    onSuccess: async (report) => {
      if (report.dryRun) return;
      await qc.invalidateQueries({ queryKey: queryKeys.registrations(tournamentId) });
      await qc.invalidateQueries({ queryKey: queryKeys.playersAll });
      await qc.invalidateQueries({ queryKey: queryKeys.clubs });
      await qc.invalidateQueries({ queryKey: ['events'] });
    },
  });
}
