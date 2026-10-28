import { Outlet } from 'react-router';

export function DisplayLayout() {
  return (
    <div className="min-h-screen bg-slate-950 p-8 text-white">
      <Outlet />
    </div>
  );
}
