export type EstadoEvento = 'BORRADOR' | 'PUBLICADO' | 'CANCELADO';

export interface OrganizadorDeEventoDTO {
  id: number;
  nombreComercial: string;
}

export interface TipoEntradaDTO {
  id: number;
  nombre: string;
  precio: number;
  cupoTotal: number;
  cupoDisponible: number;
}

export interface EventoDTO {
  id: number;
  organizador: OrganizadorDeEventoDTO;
  nombre: string;
  descripcion: string;
  fechaHora: string;
  lugar: string;
  estado: EstadoEvento;
  tiposEntrada: TipoEntradaDTO[];
}

export interface CrearTipoEntradaRequest {
  nombre: string;
  precio: number;
  cupoTotal: number;
}

export interface CrearEventoRequest {
  idProductora: number;
  nombre: string;
  descripcion: string;
  fechaHora: string;
  lugar: string;
  tiposEntrada: CrearTipoEntradaRequest[];
}

export interface DisponibilidadDTO {
  idTipoEntrada: number;
  idEvento: number;
  nombreTipoEntrada: string;
  precio: number;
  cupoDisponible: number;
  hayDisponibilidad: boolean;
}
