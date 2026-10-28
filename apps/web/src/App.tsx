const spaces = [
  {
    title: 'Administration',
    description: 'Créer les tournois, gérer les matchs et les terrains.',
    color: 'bg-sky-600',
  },
  {
    title: 'Live',
    description: 'Suivre les scores en direct depuis son téléphone.',
    color: 'bg-emerald-600',
  },
  {
    title: 'Écran TV',
    description: "Afficher l'état des terrains dans la salle.",
    color: 'bg-orange-500',
  },
];

export default function App() {
  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <header className="bg-slate-900 text-white">
        <div className="mx-auto max-w-5xl px-6 py-4">
          <p className="text-xl font-bold">FenuaSmash</p>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-12">
        <h1 className="text-3xl font-bold">Gestion de compétitions de badminton</h1>
        <p className="mt-3 max-w-2xl text-slate-600">
          Préparez vos tournois, envoyez les matchs aux arbitres et diffusez les scores en temps
          réel.
        </p>

        <div className="mt-10 grid gap-4 sm:grid-cols-3">
          {spaces.map((space) => (
            <article key={space.title} className="overflow-hidden rounded-xl bg-white shadow-sm">
              <div className={`h-2 ${space.color}`} />
              <div className="p-5">
                <h2 className="font-semibold">{space.title}</h2>
                <p className="mt-2 text-sm text-slate-600">{space.description}</p>
              </div>
            </article>
          ))}
        </div>
      </main>
    </div>
  );
}
