export type Rol = 'COMPRADOR' | 'ORGANIZADOR' | 'VALIDADOR' | 'ADMIN';

export interface UsuarioDTO {
  id: number;
  email: string;
  nombre: string;
  rol: Rol;
}

/** Respuesta de POST /api/auth/login (PAS-33). */
export interface RespuestaDeLogin {
  token: string;
  tipo: 'Bearer';
  /** ISO-8601. */
  expiraEn: string;
  usuario: UsuarioDTO;
}

export interface CrearUsuarioRequest {
  email: string;
  nombre: string;
  password: string;
  rol: Rol;
}
