import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';
import { vaciarCarrito } from '../ventas/api';
import { actualizarCarrito } from '../ventas/carritoEstado';

/**
 * Cierra la sesion desde cualquier lugar con menu de cuenta (header y dashboard de gestion).
 * El carrito se abandona ANTES de borrar las credenciales: la cookie de sesion es http-only y
 * sobrevive al logout, y otra persona en el mismo navegador recibiria 409 (CLAUDE.md, PAS-18).
 */
export function useSalir(alTerminar?: () => void) {
  const { cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const [saliendo, setSaliendo] = useState(false);

  async function salir() {
    setSaliendo(true);
    await vaciarCarrito().catch(() => undefined);
    actualizarCarrito(null);
    cerrarSesion();
    alTerminar?.();
    setSaliendo(false);
    navigate('/');
  }

  return { salir, saliendo };
}
