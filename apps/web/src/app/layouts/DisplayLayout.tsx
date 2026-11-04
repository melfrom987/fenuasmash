import { Outlet } from 'react-router';

export function DisplayLayout() {
  return (
    <div className="bg-ocean-950 min-h-screen p-8 text-white">
      <Outlet />
    </div>
  );
}
