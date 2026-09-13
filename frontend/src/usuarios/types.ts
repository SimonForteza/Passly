export type Rol = 'COMPRADOR' | 'ORGANIZADOR' | 'VALIDADOR' | 'ADMIN';

export interface UsuarioDTO {
  id: number;
  email: string;
  nombre: string;
  rol: Rol;
}

export interface CrearUsuarioRequest {
  email: string;
  nombre: string;
  password: string;
  rol: Rol;
}
