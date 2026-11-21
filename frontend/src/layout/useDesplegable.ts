import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Estado de un menú desplegable (cuenta, selector de productora). Se cierra al tocar afuera,
 * con Escape (devolviendo el foco al disparador) y al cambiar de página.
 */
export function useDesplegable() {
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

  return {
    abierto,
    alternar: () => setAbierto((a) => !a),
    cerrar: () => setAbierto(false),
    contenedor,
    disparador,
  };
}
