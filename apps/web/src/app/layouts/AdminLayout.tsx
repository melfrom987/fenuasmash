import { NavLink, Outlet } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Building03Icon, ChampionIcon, UserGroupIcon } from '@hugeicons/core-free-icons';
import { Icon } from '@/components/ui/Icon';
import { AccountPanel } from '@/features/auth/components/AccountPanel';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `flex items-center gap-2 rounded-md px-3 py-2 text-sm ${
    isActive ? 'bg-lagon-600 text-white' : 'text-ocean-200 hover:bg-ocean-900 hover:text-white'
  }`;

export function AdminLayout() {
  const { t } = useTranslation();

  return (
    <div className="flex min-h-screen">
      <aside className="bg-ocean-950 sticky top-0 flex h-screen w-56 shrink-0 flex-col p-4">
        <p className="font-display mb-6 text-lg font-semibold text-white">FenuaSmash</p>
        <nav className="flex-1 space-y-1">
          <NavLink to="/admin" end className={linkClass}>
            <Icon icon={ChampionIcon} size={18} />
            {t('nav.tournaments')}
          </NavLink>
          <NavLink to="/admin/players" className={linkClass}>
            <Icon icon={UserGroupIcon} size={18} />
            {t('nav.players')}
          </NavLink>
          <NavLink to="/admin/clubs" className={linkClass}>
            <Icon icon={Building03Icon} size={18} />
            {t('nav.clubs')}
          </NavLink>
        </nav>
        <AccountPanel />
      </aside>
      <main className="bg-sable-50 flex-1 p-6">
        <Outlet />
      </main>
    </div>
  );
}
