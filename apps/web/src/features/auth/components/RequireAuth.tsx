import { useEffect, useState, type ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router';
import { useTranslation } from 'react-i18next';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { secondaryButtonClass } from '@/components/ui/styles';
import { readSession, UNAUTHORIZED_EVENT, notifyUnauthorized } from '@/lib/session';
import { useErrorMessage } from '@/lib/use-error-message';
import { ADMIN_ROLES, loginPath } from '../access';
import { useCurrentUser, useLogout } from '../api';

const MAX_TIMEOUT = 2_147_483_647;

function Centered({ children }: { children: ReactNode }) {
  return (
    <div className="bg-sable-50 flex min-h-screen items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-4 text-center">{children}</div>
    </div>
  );
}

function Forbidden() {
  const { t } = useTranslation();
  const logout = useLogout();
  return (
    <Centered>
      <h1 className="font-display text-xl font-semibold">{t('auth.forbiddenTitle')}</h1>
      <p className="text-sable-700 text-sm">{t('auth.forbiddenText')}</p>
      <button type="button" onClick={logout} className={secondaryButtonClass}>
        {t('auth.logout')}
      </button>
    </Centered>
  );
}

export function RequireAuth() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const location = useLocation();
  const [expired, setExpired] = useState(false);
  const session = readSession();
  const expiresAt = session?.expiresAt;
  const me = useCurrentUser();

  useEffect(() => {
    const onUnauthorized = () => setExpired(true);
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  useEffect(() => {
    if (!expiresAt) return;
    const delay = Math.min(Math.max(Date.parse(expiresAt) - Date.now(), 0), MAX_TIMEOUT);
    const timer = window.setTimeout(notifyUnauthorized, delay);
    return () => window.clearTimeout(timer);
  }, [expiresAt]);

  if (!session || expired) {
    return <Navigate to={loginPath(location.pathname + location.search, expired)} replace />;
  }
  if (me.isPending) {
    return (
      <Centered>
        <p className="text-sable-600">{t('common.loading')}</p>
      </Centered>
    );
  }
  if (me.isError) {
    return (
      <Centered>
        <ErrorMessage>{errorMessage(me.error)}</ErrorMessage>
        <button type="button" onClick={() => void me.refetch()} className={secondaryButtonClass}>
          {t('auth.retry')}
        </button>
      </Centered>
    );
  }
  if (!ADMIN_ROLES.includes(me.data.role)) return <Forbidden />;
  return <Outlet />;
}
