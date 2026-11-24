import { createBrowserRouter, Navigate } from 'react-router';
import { AdminLayout } from './layouts/AdminLayout';
import { PublicLayout } from './layouts/PublicLayout';
import { DisplayLayout } from './layouts/DisplayLayout';
import { DashboardPage } from '@/pages/admin/DashboardPage';
import { LivePage } from '@/pages/live/LivePage';
import { DisplayPage } from '@/pages/display/DisplayPage';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { LoginPage } from '@/pages/LoginPage';
import { RequireAuth } from '@/features/auth/components/RequireAuth';
import { CreateTournamentPage } from '@/pages/admin/CreateTournamentPage';
import { TournamentLayout } from '@/pages/admin/tournament/TournamentLayout';
import { SettingsPage } from '@/pages/admin/tournament/SettingsPage';
import { EventsPage } from '@/pages/admin/tournament/EventsPage';
import { CreateEventPage } from '@/pages/admin/tournament/CreateEventPage';
import { EditEventPage } from '@/pages/admin/tournament/EditEventPage';
import { RegistrationsPage } from '@/pages/admin/tournament/RegistrationsPage';
import { ImportRegistrationsPage } from '@/pages/admin/tournament/ImportRegistrationsPage';
import { DrawsPage } from '@/pages/admin/tournament/DrawsPage';
import { PlayersPage } from '@/pages/admin/players/PlayersPage';
import { CreatePlayerPage } from '@/pages/admin/players/CreatePlayerPage';
import { EditPlayerPage } from '@/pages/admin/players/EditPlayerPage';
import { ClubsPage } from '@/pages/admin/clubs/ClubsPage';
import { CreateClubPage } from '@/pages/admin/clubs/CreateClubPage';
import { EditClubPage } from '@/pages/admin/clubs/EditClubPage';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/admin" replace /> },
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
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
              { path: 'events', element: <EventsPage /> },
              { path: 'events/new', element: <CreateEventPage /> },
              { path: 'events/:eventId', element: <EditEventPage /> },
              { path: 'registrations', element: <RegistrationsPage /> },
              { path: 'registrations/import', element: <ImportRegistrationsPage /> },
              { path: 'draws', element: <DrawsPage /> },
            ],
          },
          { path: 'players', element: <PlayersPage /> },
          { path: 'players/new', element: <CreatePlayerPage /> },
          { path: 'players/:playerId', element: <EditPlayerPage /> },
          { path: 'clubs', element: <ClubsPage /> },
          { path: 'clubs/new', element: <CreateClubPage /> },
          { path: 'clubs/:clubId', element: <EditClubPage /> },
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
