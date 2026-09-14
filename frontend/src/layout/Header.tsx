import { Link } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';

export function Header() {
  const { usuario, estaAutenticado, cerrarSesion } = useAuth();

  return (
    <header
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '1rem 1.5rem',
        borderBottom: '1px solid var(--color-border)',
      }}
    >
      <Link to="/" style={{ textDecoration: 'none' }}>
        <h1 style={{ fontSize: '1.5rem', color: 'var(--color-texto)' }}>Passly</h1>
      </Link>

      {estaAutenticado && usuario ? (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Link to="/perfil" style={{ color: 'var(--color-texto-secundario)', textDecoration: 'none' }}>
            {usuario.nombre} · {usuario.rol}
          </Link>
          <button
            type="button"
            onClick={cerrarSesion}
            style={{
              background: 'transparent',
              border: '1px solid var(--color-border)',
              color: 'var(--color-texto)',
              borderRadius: 'var(--radio-pill)',
              padding: '0.4rem 1rem',
              cursor: 'pointer',
            }}
          >
            Salir
          </button>
        </div>
      ) : (
        <Link
          to="/login"
          style={{
            background: 'var(--color-acento)',
            color: 'var(--color-acento-texto)',
            borderRadius: 'var(--radio-pill)',
            padding: '0.5rem 1.25rem',
            textDecoration: 'none',
            fontWeight: 600,
          }}
        >
          Ingresar
        </Link>
      )}
    </header>
  );
}
