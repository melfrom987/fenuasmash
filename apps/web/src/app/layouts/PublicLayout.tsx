import { Outlet } from 'react-router';

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-100">
      <header className="bg-white shadow-sm">
        <div className="mx-auto max-w-5xl px-4 py-3 font-semibold">FenuaSmash · Live</div>
      </header>
      <main className="mx-auto max-w-5xl p-4">
        <Outlet />
      </main>
    </div>
  );
}
