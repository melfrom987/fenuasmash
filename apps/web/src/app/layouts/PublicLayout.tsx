import { Outlet } from 'react-router';

export function PublicLayout() {
  return (
    <div className="bg-sable-50 min-h-screen">
      <header className="border-sable-200 border-b bg-white">
        <div className="font-display mx-auto max-w-5xl px-4 py-3 font-semibold">
          FenuaSmash · Live
        </div>
      </header>
      <main className="mx-auto max-w-5xl p-4">
        <Outlet />
      </main>
    </div>
  );
}
