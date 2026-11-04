import { Link } from 'react-router';

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <h1 className="font-display text-2xl font-semibold">Page introuvable</h1>
      <Link to="/admin" className="text-lagon-700 underline">
        Retour à l'administration
      </Link>
    </div>
  );
}
