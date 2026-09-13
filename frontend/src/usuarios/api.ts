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
