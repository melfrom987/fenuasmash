import { useTranslation } from 'react-i18next';
import { Logout01Icon } from '@hugeicons/core-free-icons';
import { Icon } from '@/components/ui/Icon';
import { LanguageSelect } from '@/components/ui/LanguageSelect';
import { useCurrentUser, useLogout } from '../api';

export function AccountPanel() {
  const { t } = useTranslation();
  const me = useCurrentUser();
  const logout = useLogout();

  return (
    <div className="border-ocean-900 text-ocean-200 space-y-3 border-t pt-4 text-sm">
      {me.data && (
        <div className="min-w-0">
          <p className="truncate text-white" title={me.data.email}>
            {me.data.email}
          </p>
          <p className="text-xs">{t(`auth.roles.${me.data.role}`)}</p>
        </div>
      )}
      <div>
        <p className="mb-1 text-xs">{t('common.language')}</p>
        <LanguageSelect selectClassName="w-full rounded-md border border-ocean-800 bg-ocean-900 px-2 py-1.5 text-sm text-white" />
      </div>
      <button
        type="button"
        onClick={logout}
        className="hover:bg-ocean-900 flex w-full items-center gap-2 rounded-md px-3 py-2 hover:text-white"
      >
        <Icon icon={Logout01Icon} size={18} />
        {t('auth.logout')}
      </button>
    </div>
  );
}
