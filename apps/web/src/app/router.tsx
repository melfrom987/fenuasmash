import { createBrowserRouter, Navigate } from 'react-router';
import { AdminLayout } from './layouts/AdminLayout';
import { PublicLayout } from './layouts/PublicLayout';
import { DisplayLayout } from './layouts/DisplayLayout';
import { DashboardPage } from '@/pages/admin/DashboardPage';
import { LivePage } from '@/pages/live/LivePage';
import { DisplayPage } from '@/pages/display/DisplayPage';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { CreateTournamentPage } from '@/pages/admin/CreateTournamentPage';
import { TournamentLayout } from '@/pages/admin/tournament/TournamentLayout';
import { SettingsPage } from '@/pages/admin/tournament/SettingsPage';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/admin" replace /> },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'tournaments/new', element: <CreateTournamentPage /> },
      {
        path: 'tournaments/:tournamentId',
        element: <TournamentLayout />,
        children: [
          { index: true, element: <Navigate to="settings" replace /> },
          { path: 'settings', element: <SettingsPage /> },
        ],
      },
    ],
  },
  {
    path: '/live/:tournamentId',
    element: <PublicLayout />,
    children: [{ index: true, element: <LivePage /> }],
  },
  {
    path: '/display/:tournamentId',
    element: <DisplayLayout />,
    children: [{ index: true, element: <DisplayPage /> }],
  },
  { path: '*', element: <NotFoundPage /> },
]);
