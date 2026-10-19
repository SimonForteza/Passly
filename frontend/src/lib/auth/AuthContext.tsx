import { createContext, useContext, useState, type ReactNode } from 'react';
import { obtenerUsuarioActual } from '../../usuarios/api';
import type { UsuarioDTO } from '../../usuarios/types';
import { getSesion, limpiarSesion, setSesion } from './session';

interface AuthContextValue {
  usuario: UsuarioDTO | null;
  estaAutenticado: boolean;
  iniciarSesion: (usuario: UsuarioDTO, password: string) => void;
  cerrarSesion: () => void;
  actualizarUsuario: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioDTO | null>(
    () => getSesion()?.usuario ?? null,
  );

  function iniciarSesion(usuarioNuevo: UsuarioDTO, password: string) {
    setSesion(usuarioNuevo, password);
    setUsuario(usuarioNuevo);
  }

  async function actualizarUsuario() {
    const sesion = getSesion();
    if (!sesion) return;
    const actual = await obtenerUsuarioActual();
    setSesion(actual, sesion.password);
    setUsuario(actual);
  }

  function cerrarSesion() {
    limpiarSesion();
    setUsuario(null);
  }

  return (
    <AuthContext.Provider
      value={{
        usuario,
        estaAutenticado: usuario !== null,
        iniciarSesion,
        cerrarSesion,
        actualizarUsuario,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext);
  if (!contexto)
    throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return contexto;
}
