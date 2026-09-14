import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';
import { vaciarCarrito } from '../ventas/api';

export function Header() {
  const { usuario, estaAutenticado, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const [saliendo, setSaliendo] = useState(false);

  async function salir() {
    setSaliendo(true);
    // Abandonar el carrito ANTES de borrar las credenciales. La cookie JSESSIONID es http-only y
    // sobrevive al logout; el carrito de esa sesión queda atado a este comprador, y si otra
    // persona entra en el mismo navegador el backend le responde 409
    // (CarritoDeOtroCompradorException) hasta en el DELETE, sin forma de salir hasta que venza la
    // sesión. Con credenciales todavía válidas, el DELETE invalida la sesión y la próxima nace
    // limpia. Best-effort: si falla, se cierra la sesión igual.
    await vaciarCarrito().catch(() => undefined);
    cerrarSesion();
    setSaliendo(false);
    navigate('/');
  }

  return (
    <header
      style={{
        display: 'flex',
        flexWrap: 'wrap',
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
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.75rem' }}>
          <Link className="enlace-cabecera" to="/carrito">
            Carrito
          </Link>
          <Link className="enlace-cabecera" to="/mis-ordenes">
            Mis órdenes
          </Link>
          {(usuario.rol === 'ORGANIZADOR' || usuario.rol === 'VALIDADOR') ? (
            <Link className="enlace-cabecera" to="/productoras">
              Mis productoras
            </Link>
          ) : null}
          <Link to="/perfil" style={{ color: 'var(--color-texto-secundario)', textDecoration: 'none' }}>
            {usuario.nombre} · {usuario.rol}
          </Link>
          <button
            type="button"
            onClick={salir}
            disabled={saliendo}
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
