import type { ReactElement } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../lib/auth/AuthContext';

export function RutaPrivada({ children }: { children: ReactElement }) {
  const { estaAutenticado } = useAuth();
  const location = useLocation();

  if (!estaAutenticado) {
    // Guardamos de donde venimos para que el login pueda devolver al usuario ahi despues de entrar.
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
