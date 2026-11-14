import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { obtenerUsuarioActual } from '../../usuarios/api';
import type { RespuestaDeLogin, UsuarioDTO } from '../../usuarios/types';
import {
  EVENTO_SESION_VENCIDA,
  getSesion,
  getSesionVigente,
  limpiarSesion,
  setSesion,
} from './session';

interface AuthContextValue {
  usuario: UsuarioDTO | null;
  estaAutenticado: boolean;
  /** Guarda el token que devolvio POST /api/auth/login. El password nunca llega aca. */
  iniciarSesion: (respuesta: RespuestaDeLogin) => void;
  cerrarSesion: () => void;
  actualizarUsuario: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioDTO | null>(
    // Al recargar la pagina, un token que ya vencio no cuenta como sesion: evita mostrar la app
    // como logueada para que el primer request termine en 401.
    () => getSesionVigente()?.usuario ?? null,
  );

  // httpClient avisa por este evento cuando el backend rechaza el token (vencido o adulterado).
  // RutaPrivada y App.tsx se encargan de mandar al login.
  useEffect(() => {
    const alVencer = () => setUsuario(null);
    window.addEventListener(EVENTO_SESION_VENCIDA, alVencer);
    return () => window.removeEventListener(EVENTO_SESION_VENCIDA, alVencer);
  }, []);

  function iniciarSesion({ token, expiraEn, usuario: titular }: RespuestaDeLogin) {
    setSesion({ token, expiraEn, usuario: titular });
    setUsuario(titular);
  }

  // El token no cambia cuando cambia el rol (crear productora, aceptar invitacion): el backend
  // lee el rol actualizado en cada request (ConvertidorDeJwtAAutenticacion). Aca solo se refresca
  // el UsuarioDTO que usa la UI para mostrar u ocultar opciones.
  async function actualizarUsuario() {
    if (!getSesion()) return;
    const actual = await obtenerUsuarioActual();
    const sesion = getSesion();
    if (!sesion) return;
    setSesion({ ...sesion, usuario: actual });
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
