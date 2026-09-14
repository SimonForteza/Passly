import type { CSSProperties, ReactNode } from 'react';

// Estilos compartidos por los formularios de identidad (login, registro, alta de admin), para no
// reescribir los mismos inline en cada pantalla. Todo sale de los tokens del theme (theme.css).
export const estilos: Record<string, CSSProperties> = {
  formulario: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
    maxWidth: '420px',
  },
  campo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.35rem',
  },
  etiqueta: {
    color: 'var(--color-texto-secundario)',
    fontSize: '0.9rem',
  },
  input: {
    background: 'var(--color-surface)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radio-input)',
    color: 'var(--color-texto)',
    padding: '0.6rem 0.75rem',
    fontFamily: 'inherit',
    fontSize: '1rem',
  },
  errorCampo: {
    color: 'var(--color-acento)',
    fontSize: '0.8rem',
  },
  boton: {
    background: 'var(--color-acento)',
    color: 'var(--color-acento-texto)',
    border: 'none',
    borderRadius: 'var(--radio-pill)',
    padding: '0.65rem 1.25rem',
    fontWeight: 600,
    fontSize: '1rem',
    cursor: 'pointer',
  },
  botonDeshabilitado: {
    opacity: 0.6,
    cursor: 'progress',
  },
};

interface CampoTextoProps {
  etiqueta: string;
  tipo?: 'text' | 'email' | 'password';
  valor: string;
  onCambio: (valor: string) => void;
  autoComplete?: string;
  requerido?: boolean;
  error?: string;
}

export function CampoTexto({
  etiqueta,
  tipo = 'text',
  valor,
  onCambio,
  autoComplete,
  requerido,
  error,
}: CampoTextoProps) {
  return (
    <label style={estilos.campo}>
      <span style={estilos.etiqueta}>{etiqueta}</span>
      <input
        type={tipo}
        value={valor}
        onChange={(e) => onCambio(e.target.value)}
        autoComplete={autoComplete}
        required={requerido}
        style={estilos.input}
      />
      {error && <span style={estilos.errorCampo}>{error}</span>}
    </label>
  );
}

export function BotonPrimario({
  cargando,
  children,
}: {
  cargando: boolean;
  children: ReactNode;
}) {
  return (
    <button
      type="submit"
      disabled={cargando}
      style={{ ...estilos.boton, ...(cargando ? estilos.botonDeshabilitado : null) }}
    >
      {children}
    </button>
  );
}
