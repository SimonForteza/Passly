import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import type { UsuarioDTO } from '../usuarios/types';
import { useCantidadCarrito } from '../ventas/carritoEstado';
import { useDesplegable } from './useDesplegable';

function IconoPersona() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
      <circle cx="12" cy="8" r="4" fill="currentColor" />
      <path d="M4 20c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5" fill="currentColor" />
    </svg>
  );
}

/**
 * Avatar + nombre. Al tocarlo abre la cajita de la cuenta (perfil, entradas, salir).
 * En el header abre hacia abajo; al pie del menú lateral de gestión, hacia arriba.
 */
export function MenuDeCuenta({
  usuario,
  saliendo,
  onSalir,
  haciaArriba = false,
}: {
  usuario: UsuarioDTO;
  saliendo: boolean;
  onSalir: () => void;
  haciaArriba?: boolean;
}) {
  const { abierto, alternar, cerrar, contenedor, disparador } = useDesplegable();
  // "Salir" no cierra la sesión en el acto: primero pide confirmación (mismo patrón <dialog>
  // que publicar y cancelar un evento).
  const dialogo = useRef<HTMLDialogElement>(null);
  const [confirmando, setConfirmando] = useState(false);
  const enCarrito = useCantidadCarrito();

  useEffect(() => {
    if (confirmando) dialogo.current?.showModal();
    else dialogo.current?.close();
  }, [confirmando]);

  function pedirConfirmacion() {
    cerrar();
    setConfirmando(true);
  }

  return (
    <div className={'cuenta' + (haciaArriba ? ' cuenta-arriba' : '')} ref={contenedor}>
      <button
        ref={disparador}
        type="button"
        className="cuenta-disparador"
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-controls="menu-de-cuenta"
        aria-label={`Cuenta de ${usuario.nombre}`}
        onClick={alternar}
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
          <Link role="menuitem" className="cuenta-menu-item" to="/perfil" onClick={cerrar}>
            Mi perfil
          </Link>
          <Link role="menuitem" className="cuenta-menu-item" to="/mis-ordenes" onClick={cerrar}>
            Mis entradas
          </Link>
          <hr className="cuenta-menu-separador" />
          <button
            role="menuitem"
            type="button"
            className="cuenta-menu-item cuenta-menu-salir"
            onClick={pedirConfirmacion}
          >
            Salir
          </button>
        </div>
      ) : null}

      <dialog
        ref={dialogo}
        className="dialogo-publicacion dialogo-confirmacion"
        aria-labelledby="confirmar-salida"
        onCancel={(e) => {
          if (saliendo) e.preventDefault();
          else setConfirmando(false);
        }}
        onClose={() => setConfirmando(false)}
      >
        <div className="stack-grande">
          <h2 id="confirmar-salida">¿Querés cerrar sesión?</h2>
          <p>
            {enCarrito > 0
              ? `Tenés ${enCarrito === 1 ? '1 entrada' : enCarrito + ' entradas'} en el carrito. Si salís, se descartan.`
              : 'Vas a tener que volver a ingresar para ver tus entradas o gestionar tus eventos.'}
          </p>
          <div className="acciones">
            <button className="boton boton-primario" disabled={saliendo} onClick={onSalir}>
              {saliendo ? 'Saliendo…' : 'Cerrar sesión'}
            </button>
            <button
              className="boton boton-secundario"
              disabled={saliendo}
              onClick={() => {
                setConfirmando(false);
                disparador.current?.focus();
              }}
            >
              Cancelar
            </button>
          </div>
        </div>
      </dialog>
    </div>
  );
}
