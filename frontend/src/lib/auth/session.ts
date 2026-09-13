import type { UsuarioDTO } from '../../usuarios/types';

// Funciones planas (no React) sobre sessionStorage, para que httpClient pueda leer las
// credenciales sin importar AuthContext. sessionStorage y no localStorage: el password
// vive en claro del lado del cliente porque el backend es HTTP Basic puro (sin JWT), así
// que conviene limitar su vida a la pestaña actual en vez de persistirlo entre sesiones.
const CLAVE_SESION = 'passly.sesion';

interface SesionGuardada {
  usuario: UsuarioDTO;
  password: string;
}

export function getSesion(): SesionGuardada | null {
  const crudo = sessionStorage.getItem(CLAVE_SESION);
  if (!crudo) return null;
  try {
    return JSON.parse(crudo) as SesionGuardada;
  } catch {
    return null;
  }
}

export function setSesion(usuario: UsuarioDTO, password: string): void {
  sessionStorage.setItem(CLAVE_SESION, JSON.stringify({ usuario, password }));
}

export function limpiarSesion(): void {
  sessionStorage.removeItem(CLAVE_SESION);
}

export function getHeaderAutorizacion(): string | null {
  const sesion = getSesion();
  if (!sesion) return null;
  const credenciales = btoa(`${sesion.usuario.email}:${sesion.password}`);
  return `Basic ${credenciales}`;
}
