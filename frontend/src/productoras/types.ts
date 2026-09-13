export type RolEnProductora = 'DUENIO' | 'STAFF' | 'VALIDADOR';

export interface ProductoraDTO {
  id: number;
  nombreComercial: string;
  cuit: string;
  descripcion?: string;
  logoUrl?: string;
}

export interface CrearProductoraRequest {
  nombreComercial: string;
  cuit?: string;
  descripcion?: string;
  logoUrl?: string;
}

export interface MiembroDTO {
  idUsuario: number;
  email: string;
  nombre: string;
  rolEnProductora: RolEnProductora;
}

export interface AgregarMiembroRequest {
  idUsuario: number;
  rolEnProductora: RolEnProductora;
}
