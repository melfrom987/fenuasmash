import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import type { CurrentUser, LoginRequest, LoginResponse } from '@/api/models';
import { api } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import { clearSession, readSession, saveSession } from '@/lib/session';

const fetchCurrentUser = () => api<CurrentUser>('/auth/me');

export function useCurrentUser() {
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: fetchCurrentUser,
    enabled: readSession() !== null,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}

export function useLogin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: LoginRequest) => {
      const response = await api<LoginResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify(input),
      });
      saveSession({ accessToken: response.accessToken, expiresAt: response.expiresAt });
      qc.clear();
      return qc.fetchQuery({ queryKey: queryKeys.me, queryFn: fetchCurrentUser });
    },
  });
}

export function useLogout() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  return () => {
    clearSession();
    qc.clear();
    void navigate('/login', { replace: true });
  };
}
