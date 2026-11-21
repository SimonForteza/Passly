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
  /** Ruta versionada de la portada (PAS-47), o null si el evento no tiene. */
  portadaUrl: string | null;
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

// Sin idProductora: un evento no cambia de dueño (PAS-19).
export interface EditarEventoRequest {
  nombre: string;
  descripcion: string;
  fechaHora: string;
  lugar: string;
}

export interface EditarTipoEntradaRequest {
  nombre: string;
  precio: number;
  cupoTotal: number;
}

export interface AmpliarCupoRequest {
  cantidad: number;
}

export interface DisponibilidadDTO {
  idTipoEntrada: number;
  idEvento: number;
  nombreTipoEntrada: string;
  precio: number;
  cupoDisponible: number;
  hayDisponibilidad: boolean;
}
