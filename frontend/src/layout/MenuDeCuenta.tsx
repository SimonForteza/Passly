import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { UsuarioDTO } from '../usuarios/types';

function IconoPersona() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
      <circle cx="12" cy="8" r="4" fill="currentColor" />
      <path d="M4 20c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5" fill="currentColor" />
    </svg>
  );
}

/**
 * Avatar + nombre en el header. Al tocarlo abre la cajita de la cuenta (perfil, entradas, salir).
 * Se cierra al tocar afuera, con Escape, al elegir una opción o al cambiar de página.
 */
export function MenuDeCuenta({
  usuario,
  saliendo,
  onSalir,
}: {
  usuario: UsuarioDTO;
  saliendo: boolean;
  onSalir: () => void;
}) {
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef<HTMLDivElement>(null);
  const disparador = useRef<HTMLButtonElement>(null);
  const { pathname } = useLocation();

  // Cambiar de página cierra el menú (ajuste de estado durante el render, sin efecto).
  const [rutaAnterior, setRutaAnterior] = useState(pathname);
  if (rutaAnterior !== pathname) {
    setRutaAnterior(pathname);
    setAbierto(false);
  }

  useEffect(() => {
    if (!abierto) return;
    function alTocarAfuera(e: MouseEvent | TouchEvent) {
      if (!contenedor.current?.contains(e.target as Node)) setAbierto(false);
    }
    function alPresionarTecla(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        setAbierto(false);
        disparador.current?.focus();
      }
    }
    document.addEventListener('mousedown', alTocarAfuera);
    document.addEventListener('touchstart', alTocarAfuera);
    document.addEventListener('keydown', alPresionarTecla);
    return () => {
      document.removeEventListener('mousedown', alTocarAfuera);
      document.removeEventListener('touchstart', alTocarAfuera);
      document.removeEventListener('keydown', alPresionarTecla);
    };
  }, [abierto]);

  return (
    <div className="cuenta" ref={contenedor}>
      <button
        ref={disparador}
        type="button"
        className="cuenta-disparador"
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-controls="menu-de-cuenta"
        aria-label={`Cuenta de ${usuario.nombre}`}
        onClick={() => setAbierto(!abierto)}
      >
        <span className="cuenta-avatar">
          <IconoPersona />
        </span>
        <span className="cuenta-nombre">{usuario.nombre}</span>
      </button>

      {abierto ? (
        <div id="menu-de-cuenta" className="cuenta-menu" role="menu">
          <div className="cuenta-menu-cabecera">
            <strong>{usuario.nombre}</strong>
            <span className="texto-secundario">{usuario.email}</span>
          </div>
          <Link
            role="menuitem"
            className="cuenta-menu-item"
            to="/perfil"
            onClick={() => setAbierto(false)}
          >
            Mi perfil
          </Link>
          <Link
            role="menuitem"
            className="cuenta-menu-item"
            to="/mis-ordenes"
            onClick={() => setAbierto(false)}
          >
            Mis entradas
          </Link>
          <hr className="cuenta-menu-separador" />
          <button
            role="menuitem"
            type="button"
            className="cuenta-menu-item cuenta-menu-salir"
            disabled={saliendo}
            onClick={onSalir}
          >
            {saliendo ? 'Saliendo…' : 'Salir'}
          </button>
        </div>
      ) : null}
    </div>
  );
}
