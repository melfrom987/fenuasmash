import { NavLink, Outlet } from 'react-router';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `block rounded-md px-3 py-2 text-sm ${
    isActive ? 'bg-slate-800 text-white' : 'text-slate-300 hover:bg-slate-800'
  }`;

export function AdminLayout() {
  return (
    <div className="flex min-h-screen">
      <aside className="w-56 shrink-0 bg-slate-900 p-4">
        <p className="mb-6 text-lg font-semibold text-white">FenuaSmash</p>
        <nav className="space-y-1">
          <NavLink to="/admin" end className={linkClass}>
            Tournois
          </NavLink>
        </nav>
      </aside>
      <main className="flex-1 bg-slate-100 p-6">
        <Outlet />
      </main>
    </div>
  );
}
