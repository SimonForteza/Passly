import { construirHeaderBasic } from '../lib/auth/session';
import { httpClient } from '../lib/http/httpClient';
import type { CrearUsuarioRequest, UsuarioDTO } from './types';

export function crearUsuario(request: CrearUsuarioRequest): Promise<UsuarioDTO> {
  return httpClient.post<UsuarioDTO>('/api/usuarios', request);
}

export function obtenerUsuario(id: number): Promise<UsuarioDTO> {
  return httpClient.get<UsuarioDTO>(`/api/usuarios/${id}`);
}

export function listarUsuarios(): Promise<UsuarioDTO[]> {
  return httpClient.get<UsuarioDTO[]>('/api/usuarios');
}

/**
 * "Quien soy": el UsuarioDTO del autenticado. Con credenciales explicitas valida un login por
 * email+clave (el header Basic va armado a mano porque todavia no hay sesion guardada); sin
 * ellas usa la sesion actual. Un 401 => credenciales invalidas.
 */
export function obtenerUsuarioActual(credenciales?: { email: string; password: string }): Promise<UsuarioDTO> {
  const init = credenciales
    ? { headers: { Authorization: construirHeaderBasic(credenciales.email, credenciales.password) } }
    : undefined;
  return httpClient.get<UsuarioDTO>('/api/usuarios/me', init);
}
