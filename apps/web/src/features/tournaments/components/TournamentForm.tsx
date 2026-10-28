import type { ReactNode } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import type { Organization } from '@/api/models';
import { Button } from '@/components/ui/Button';
import { tournamentFormSchema, type TournamentFormValues } from '../schema';

const inputClass = 'mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm';

export function TournamentForm({
  organizations,
  onSubmit,
  submitting,
}: {
  organizations: Organization[];
  onSubmit: (values: TournamentFormValues) => void;
  submitting: boolean;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TournamentFormValues>({
    resolver: zodResolver(tournamentFormSchema),
    defaultValues: {
      organizationId: organizations[0]?.id ?? '',
      name: '',
      startsAt: '',
      endsAt: '',
      minimumRestMinutes: 20,
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <Field label="Organisation" error={errors.organizationId?.message}>
        <select className={inputClass} {...register('organizationId')}>
          {organizations.map((o) => (
            <option key={o.id} value={o.id}>
              {o.name}
            </option>
          ))}
        </select>
      </Field>

      <Field label="Nom du tournoi" error={errors.name?.message}>
        <input className={inputClass} placeholder="Open de Papeete" {...register('name')} />
      </Field>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Début" error={errors.startsAt?.message}>
          <input type="datetime-local" className={inputClass} {...register('startsAt')} />
        </Field>
        <Field label="Fin" error={errors.endsAt?.message}>
          <input type="datetime-local" className={inputClass} {...register('endsAt')} />
        </Field>
      </div>

      <Field
        label="Repos minimum entre deux matchs (minutes)"
        error={errors.minimumRestMinutes?.message}
      >
        <input
          type="number"
          min={0}
          className={inputClass}
          {...register('minimumRestMinutes', { valueAsNumber: true })}
        />
      </Field>

      <Button type="submit" disabled={submitting}>
        {submitting ? 'Création…' : 'Créer le tournoi'}
      </Button>
    </form>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      {children}
      {error && <span className="mt-1 block text-sm text-red-700">{error}</span>}
    </label>
  );
}
