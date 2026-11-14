import type { UsuarioDTO } from '../../usuarios/types';

// Funciones planas (no React) sobre sessionStorage, para que httpClient pueda leer el token sin
// importar AuthContext.
//
// Desde PAS-34 se guarda el JWT que devuelve POST /api/auth/login, NUNCA el password: la clave sale
// de la pantalla de login en un unico request y no queda en el navegador. Con HTTP Basic habia que
// guardarla en claro para re-armar el header en cada request; ese era el costo que se fue.
// sessionStorage y no localStorage: el token muere con la pestaña (menos ventana si alguien la
// deja abierta) y no hay refresh token, asi que al vencer se vuelve a iniciar sesion.
const CLAVE_SESION = 'passly.sesion';

/** Evento de window que avisa que el backend rechazo el token guardado (vencido o adulterado). */
export const EVENTO_SESION_VENCIDA = 'passly:sesion-vencida';

export interface SesionGuardada {
  usuario: UsuarioDTO;
  token: string;
  /** ISO-8601, tal como lo devuelve el backend. */
  expiraEn: string;
}

export function getSesion(): SesionGuardada | null {
  const crudo = sessionStorage.getItem(CLAVE_SESION);
  if (!crudo) return null;
  try {
    const sesion = JSON.parse(crudo) as Partial<SesionGuardada>;
    // Una sesion con otra forma (por ejemplo la vieja de Basic, con password) no sirve: se
    // descarta para que el password no quede olvidado en sessionStorage.
    if (typeof sesion.token !== 'string' || !sesion.usuario) {
      limpiarSesion();
      return null;
    }
    return sesion as SesionGuardada;
  } catch {
    limpiarSesion();
    return null;
  }
}

/** Sesion guardada y todavia no vencida segun su expiraEn; si ya vencio, la borra. */
export function getSesionVigente(): SesionGuardada | null {
  const sesion = getSesion();
  if (!sesion) return null;
  const vence = Date.parse(sesion.expiraEn);
  if (Number.isNaN(vence) || vence <= Date.now()) {
    limpiarSesion();
    return null;
  }
  return sesion;
}

export function setSesion(sesion: SesionGuardada): void {
  sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
}

export function limpiarSesion(): void {
  sessionStorage.removeItem(CLAVE_SESION);
}

export function getToken(): string | null {
  return getSesion()?.token ?? null;
}

/** Borra la sesion y avisa a AuthContext y al router (ver App.tsx). */
export function avisarSesionVencida(): void {
  limpiarSesion();
  window.dispatchEvent(new Event(EVENTO_SESION_VENCIDA));
}
