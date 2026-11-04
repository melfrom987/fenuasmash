import { NavLink, Outlet } from 'react-router';
import { ChampionIcon } from '@hugeicons/core-free-icons';
import { Icon } from '@/components/ui/Icon';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `flex items-center gap-2 rounded-md px-3 py-2 text-sm ${
    isActive ? 'bg-lagon-600 text-white' : 'text-ocean-200 hover:bg-ocean-900 hover:text-white'
  }`;

export function AdminLayout() {
  return (
    <div className="flex min-h-screen">
      <aside className="bg-ocean-950 w-56 shrink-0 p-4">
        <p className="font-display mb-6 text-lg font-semibold text-white">FenuaSmash</p>
        <nav className="space-y-1">
          <NavLink to="/admin" end className={linkClass}>
            <Icon icon={ChampionIcon} size={18} />
            Tournois
          </NavLink>
        </nav>
      </aside>
      <main className="bg-sable-50 flex-1 p-6">
        <Outlet />
      </main>
    </div>
  );
}
