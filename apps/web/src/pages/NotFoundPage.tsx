import { Link } from 'react-router';

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <h1 className="text-2xl font-semibold">Page introuvable</h1>
      <Link to="/admin" className="text-blue-600 underline">
        Retour à l'administration
      </Link>
    </div>
  );
}
