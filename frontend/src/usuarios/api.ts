import { httpClient } from '../lib/http/httpClient';
import type { CrearUsuarioRequest, RespuestaDeLogin, UsuarioDTO } from './types';

export function crearUsuario(request: CrearUsuarioRequest): Promise<UsuarioDTO> {
  return httpClient.post<UsuarioDTO>('/api/usuarios', request);
}

export function obtenerUsuario(id: number): Promise<UsuarioDTO> {
  return httpClient.get<UsuarioDTO>(`/api/usuarios/${id}`);
}

export function listarUsuarios(): Promise<UsuarioDTO[]> {
  return httpClient.get<UsuarioDTO[]>('/api/usuarios');
}

/** "Quien soy": el UsuarioDTO del titular del token guardado. */
export function obtenerUsuarioActual(): Promise<UsuarioDTO> {
  return httpClient.get<UsuarioDTO>('/api/usuarios/me');
}

/**
 * POST /api/auth/login (PAS-33): canjea email + password por un JWT. Unico request en el que viaja
 * el password; despues solo viaja el token. Credenciales invalidas => 401, con el mismo mensaje
 * exista o no la cuenta.
 */
export function iniciarSesion(email: string, password: string): Promise<RespuestaDeLogin> {
  return httpClient.post<RespuestaDeLogin>('/api/auth/login', { email, password }, { anonimo: true });
}
