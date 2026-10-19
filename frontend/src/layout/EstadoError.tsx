import { ApiError } from '../lib/http/ApiError';

export function EstadoError({ error }: { error: unknown }) {
  const mensaje =
    error instanceof ApiError
      ? error.status === 401
        ? 'Tu sesión no está disponible. Ingresá nuevamente para continuar.'
        : error.status === 403 && !error.detail?.includes('origen de la compra')
          ? 'Tu cuenta no tiene acceso a esta acción. Revisá la cuenta y la productora seleccionadas.'
          : error.detail ||
            error.title ||
            'No pudimos completar la acción. Intentá de nuevo.'
      : 'No pudimos completar la acción. Comprobá tu conexión e intentá de nuevo.';

  return (
    <p
      role="alert"
      style={{
        color: 'var(--color-texto)',
        background: 'rgba(232, 17, 45, 0.12)',
        border: '1px solid var(--color-acento)',
        borderRadius: 'var(--radio-input)',
        padding: '0.75rem 1rem',
      }}
    >
      {mensaje}
    </p>
  );
}
