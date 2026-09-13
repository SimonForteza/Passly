import { createContext, useContext, useState, type ReactNode } from 'react';
import type { UsuarioDTO } from '../../usuarios/types';
import { getSesion, limpiarSesion, setSesion } from './session';

interface AuthContextValue {
  usuario: UsuarioDTO | null;
  estaAutenticado: boolean;
  iniciarSesion: (usuario: UsuarioDTO, password: string) => void;
  cerrarSesion: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioDTO | null>(() => getSesion()?.usuario ?? null);

  function iniciarSesion(usuarioNuevo: UsuarioDTO, password: string) {
    setSesion(usuarioNuevo, password);
    setUsuario(usuarioNuevo);
  }

  function cerrarSesion() {
    limpiarSesion();
    setUsuario(null);
  }

  return (
    <AuthContext.Provider
      value={{ usuario, estaAutenticado: usuario !== null, iniciarSesion, cerrarSesion }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext);
  if (!contexto) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return contexto;
}
