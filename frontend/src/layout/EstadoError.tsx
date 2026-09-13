import { ApiError } from '../lib/http/ApiError';

export function EstadoError({ error }: { error: unknown }) {
  const mensaje = error instanceof ApiError ? error.detail || error.title : 'Ocurrió un error inesperado.';

  return (
    <p
      role="alert"
      style={{
        color: 'var(--color-texto)',
        background: 'rgba(232, 18, 58, 0.12)',
        border: '1px solid var(--color-acento)',
        borderRadius: 'var(--radio-input)',
        padding: '0.75rem 1rem',
      }}
    >
      {mensaje}
    </p>
  );
}
