import { createBrowserRouter, Navigate } from 'react-router';
import { AdminLayout } from './layouts/AdminLayout';
import { PublicLayout } from './layouts/PublicLayout';
import { DisplayLayout } from './layouts/DisplayLayout';
import { DashboardPage } from '@/pages/admin/DashboardPage';
import { LivePage } from '@/pages/live/LivePage';
import { DisplayPage } from '@/pages/display/DisplayPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/admin" replace /> },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [{ index: true, element: <DashboardPage /> }],
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
