import { createBrowserRouter } from 'react-router-dom';
import { AppLayout } from './layout/AppLayout';
import { RutaPrivada } from './layout/RutaPrivada';
import { CarteleraPage } from './eventos/CarteleraPage';
import { DetalleEventoPage } from './eventos/DetalleEventoPage';
import { LoginPage } from './usuarios/LoginPage';
import { RegistroPage } from './usuarios/RegistroPage';
import { CarritoPage } from './ventas/CarritoPage';
import { MisOrdenesPage } from './ventas/MisOrdenesPage';
import { BackofficePage } from './productoras/BackofficePage';
import { ProductorasPage } from './productoras/ProductorasPage';

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      // Públicas
      { path: '/', element: <CarteleraPage /> },
      { path: '/eventos/:id', element: <DetalleEventoPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/registro', element: <RegistroPage /> },

      // Privadas
      {
        path: '/carrito',
        element: (
          <RutaPrivada>
            <CarritoPage />
          </RutaPrivada>
        ),
      },
      {
        path: '/mis-ordenes',
        element: (
          <RutaPrivada>
            <MisOrdenesPage />
          </RutaPrivada>
        ),
      },
      {
        path: '/productoras',
        element: (
          <RutaPrivada>
            <ProductorasPage />
          </RutaPrivada>
        ),
      },
      {
        path: '/productoras/:id/backoffice',
        element: (
          <RutaPrivada>
            <BackofficePage />
          </RutaPrivada>
        ),
      },
    ],
  },
]);
