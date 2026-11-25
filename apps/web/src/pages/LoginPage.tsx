import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Navigate, useNavigate, useSearchParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { Button } from '@/components/ui/Button';
import { ErrorMessage } from '@/components/ui/ErrorMessage';
import { Field } from '@/components/ui/Field';
import { LanguageSelect } from '@/components/ui/LanguageSelect';
import { inputClass } from '@/components/ui/styles';
import { safeRedirect } from '@/features/auth/access';
import { useLogin } from '@/features/auth/api';
import { key } from '@/i18n';
import { readSession } from '@/lib/session';
import { useErrorMessage } from '@/lib/use-error-message';

const loginSchema = z.object({
  email: z.string().trim().min(1, key('auth.errors.emailRequired')),
  password: z.string().min(1, key('auth.errors.passwordRequired')),
});

type LoginValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const { t } = useTranslation();
  const errorMessage = useErrorMessage();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirect = safeRedirect(searchParams.get('redirect'));
  const expired = searchParams.get('expired') === '1';
  const login = useLogin();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  if (readSession() && !login.isPending) return <Navigate to={redirect} replace />;

  const submit = handleSubmit((values) =>
    login.mutate(values, { onSuccess: () => void navigate(redirect, { replace: true }) }),
  );

  return (
    <div className="bg-sable-50 flex min-h-screen flex-col">
      <header className="flex justify-end p-4">
        <div className="w-36">
          <LanguageSelect />
        </div>
      </header>
      <main className="flex flex-1 items-start justify-center px-4 pt-[10vh] pb-16">
        <div className="w-full max-w-sm space-y-6">
          <div className="text-center">
            <p className="font-display text-ocean-950 text-2xl font-semibold">FenuaSmash</p>
            <h1 className="text-sable-600 mt-1 text-sm">{t('auth.title')}</h1>
          </div>

          <form
            onSubmit={submit}
            noValidate
            className="space-y-4 rounded-lg bg-white p-6 shadow-sm"
          >
            {expired && !login.error && (
              <p className="bg-sunset-50 text-sunset-800 rounded-md px-3 py-2 text-sm">
                {t('auth.expired')}
              </p>
            )}
            {login.error && <ErrorMessage>{errorMessage(login.error)}</ErrorMessage>}

            <Field label={t('auth.email')} error={errors.email?.message}>
              <input
                type="email"
                autoComplete="username"
                autoFocus
                {...register('email')}
                className={inputClass}
              />
            </Field>
            <Field label={t('auth.password')} error={errors.password?.message}>
              <input
                type="password"
                autoComplete="current-password"
                {...register('password')}
                className={inputClass}
              />
            </Field>

            <Button type="submit" disabled={login.isPending} className="w-full">
              {login.isPending ? t('auth.submitting') : t('auth.submit')}
            </Button>
          </form>

          {import.meta.env.MODE === 'mock' && (
            <p className="text-sable-600 text-center text-xs">{t('auth.mockHint')}</p>
          )}
        </div>
      </main>
    </div>
  );
}
