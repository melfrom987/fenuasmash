import { useQuery } from '@tanstack/react-query';
import type { Organization } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export function useOrganizations() {
  return useQuery({
    queryKey: queryKeys.organizations,
    queryFn: () => api<Organization[]>('/organizations'),
  });
}
