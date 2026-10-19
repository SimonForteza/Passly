import { createBrowserRouter } from 'react-router-dom';
import { AppLayout } from './layout/AppLayout';
import { RutaPrivada } from './layout/RutaPrivada';
import { CarteleraPage } from './eventos/CarteleraPage';
import { DetalleEventoPage } from './eventos/DetalleEventoPage';
import { LoginPage } from './usuarios/LoginPage';
import { RegistroPage } from './usuarios/RegistroPage';
import { PerfilPage } from './usuarios/PerfilPage';
import { MisOrdenesPage } from './ventas/MisOrdenesPage';
import { DetalleOrdenPage } from './ventas/DetalleOrdenPage';
import { CarritoSegunSesion } from './ventas/CarritoSegunSesion';
import { OrdenInvitadoPage } from './ventas/OrdenInvitadoPage';
import { AceptarInvitacionPage } from './productoras/AceptarInvitacionPage';
import { BackofficePage } from './productoras/BackofficePage';
import { PaginaProductoraPage } from './productoras/PaginaProductoraPage';
import { ProductorasPage } from './productoras/ProductorasPage';
import { CrearProductoraPage } from './productoras/CrearProductoraPage';
import { OrganizarPage } from './productoras/OrganizarPage';
import { ResumenProductoraPage } from './productoras/ResumenProductoraPage';
import { GestionLayout } from './productoras/GestionLayout';
import { CrearEventoPage } from './eventos/CrearEventoPage';
import { GestionEventoPage } from './eventos/GestionEventoPage';
import { EditarEventoPage } from './eventos/EditarEventoPage';
import { EventosDeProductoraPage } from './eventos/EventosDeProductoraPage';
import { ValidarPage } from './accesos/ValidarPage';

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <CarteleraPage /> },
      { path: '/eventos/:id', element: <DetalleEventoPage /> },
      { path: '/organizar', element: <OrganizarPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/registro', element: <RegistroPage /> },
      { path: '/invitaciones', element: <AceptarInvitacionPage /> },
      { path: '/orden-invitado', element: <OrdenInvitadoPage /> },
      { path: '/carrito', element: <CarritoSegunSesion /> },
      {
        path: '/perfil',
        element: (
          <RutaPrivada>
            <PerfilPage />
          </RutaPrivada>
        ),
      },
      {
        path: '/validar',
        element: (
          <RutaPrivada>
            <ValidarPage />
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
        path: '/mis-ordenes/:id',
        element: (
          <RutaPrivada>
            <DetalleOrdenPage />
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
        path: '/productoras/nueva',
        element: (
          <RutaPrivada>
            <CrearProductoraPage />
          </RutaPrivada>
        ),
      },
      {
        path: '/productoras/:id',
        children: [
          { index: true, element: <PaginaProductoraPage /> },
          {
            element: (
              <RutaPrivada>
                <GestionLayout />
              </RutaPrivada>
            ),
            children: [
              { path: 'gestion', element: <ResumenProductoraPage /> },
              { path: 'backoffice', element: <BackofficePage /> },
              { path: 'eventos', element: <EventosDeProductoraPage /> },
              { path: 'eventos/nuevo', element: <CrearEventoPage /> },
              { path: 'eventos/:idEvento', element: <GestionEventoPage /> },
              {
                path: 'eventos/:idEvento/editar',
                element: <EditarEventoPage />,
              },
            ],
          },
        ],
      },
    ],
  },
]);
