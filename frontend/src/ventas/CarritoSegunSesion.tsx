import { useAuth } from '../lib/auth/AuthContext';
import { CarritoInvitadoPage } from './CarritoInvitadoPage';
import { CarritoPage } from './CarritoPage';
export function CarritoSegunSesion() {
  const { estaAutenticado } = useAuth();
  return estaAutenticado ? <CarritoPage /> : <CarritoInvitadoPage />;
}
