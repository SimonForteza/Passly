import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { AuthProvider } from './lib/auth/AuthContext';
import { EVENTO_SESION_VENCIDA } from './lib/auth/session';
import { router } from './router';
import { OrganizacionProvider } from './productoras/OrganizacionContext';
import { actualizarCarrito } from './ventas/carritoEstado';

// El AuthProvider envuelve al RouterProvider, asi que no puede usar useNavigate: la redireccion
// por sesion vencida se hace aca, con el router de datos. RutaPrivada ya manda al login a quien
// pierde la sesion en una pantalla privada; esto cubre ademas las publicas (cartelera, detalle,
// carrito), donde el backend tambien responde 401 a un token invalido.
function redirigirAlLoginSiVencio() {
  const { location } = router.state;
  actualizarCarrito(null);
  if (location.pathname === '/login') return;
  void router.navigate('/login', {
    replace: true,
    state: {
      from: location,
      contexto: 'Tu sesión venció o ya no es válida. Ingresá de nuevo para continuar.',
    },
  });
}

export default function App() {
  useEffect(() => {
    window.addEventListener(EVENTO_SESION_VENCIDA, redirigirAlLoginSiVencio);
    return () => window.removeEventListener(EVENTO_SESION_VENCIDA, redirigirAlLoginSiVencio);
  }, []);

  return (
    <AuthProvider>
      <OrganizacionProvider>
        <RouterProvider router={router} />
      </OrganizacionProvider>
    </AuthProvider>
  );
}
