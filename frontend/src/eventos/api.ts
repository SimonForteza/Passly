import { httpClient } from '../lib/http/httpClient';
import type {
  AmpliarCupoRequest,
  CrearEventoRequest,
  CrearTipoEntradaRequest,
  DisponibilidadDTO,
  EditarEventoRequest,
  EditarTipoEntradaRequest,
  EventoDTO,
} from './types';

export function listarEventos(idProductora?: number): Promise<EventoDTO[]> {
  const query = idProductora ? `?productora=${idProductora}` : '';
  return httpClient.get<EventoDTO[]>(`/api/eventos${query}`);
}

export function obtenerEvento(id: number): Promise<EventoDTO> {
  return httpClient.get<EventoDTO>(`/api/eventos/${id}`);
}

export function crearEvento(request: CrearEventoRequest): Promise<EventoDTO> {
  return httpClient.post<EventoDTO>('/api/eventos', request);
}

export function publicarEvento(id: number): Promise<EventoDTO> {
  return httpClient.post<EventoDTO>(`/api/eventos/${id}/publicacion`);
}

/** Transición a CANCELADO (PAS-35). Cancelar un evento ya cancelado responde 409. */
export function cancelarEvento(id: number): Promise<EventoDTO> {
  return httpClient.post<EventoDTO>(`/api/eventos/${id}/cancelacion`);
}

export function listarEventosDeProductora(idProductora: number): Promise<EventoDTO[]> {
  return httpClient.get<EventoDTO[]>(`/api/productoras/${idProductora}/eventos`);
}

export function consultarDisponibilidad(idTipoEntrada: number): Promise<DisponibilidadDTO> {
  return httpClient.get<DisponibilidadDTO>(`/api/tipos-entrada/${idTipoEntrada}/disponibilidad`);
}

export function editarEvento(id: number, request: EditarEventoRequest): Promise<EventoDTO> {
  return httpClient.put<EventoDTO>(`/api/eventos/${id}`, request);
}

export function agregarTipoEntrada(
  idEvento: number,
  request: CrearTipoEntradaRequest,
): Promise<EventoDTO> {
  return httpClient.post<EventoDTO>(`/api/eventos/${idEvento}/tipos-entrada`, request);
}

export function editarTipoEntrada(
  idEvento: number,
  idTipo: number,
  request: EditarTipoEntradaRequest,
): Promise<EventoDTO> {
  return httpClient.put<EventoDTO>(`/api/eventos/${idEvento}/tipos-entrada/${idTipo}`, request);
}

export function ampliarCupo(
  idEvento: number,
  idTipo: number,
  request: AmpliarCupoRequest,
): Promise<EventoDTO> {
  return httpClient.post<EventoDTO>(
    `/api/eventos/${idEvento}/tipos-entrada/${idTipo}/ampliacion-de-cupo`,
    request,
  );
}

/** Sube o reemplaza la portada (PAS-47). El backend valida tipo (JPG, PNG, WebP) y tamaño (2 MB). */
export function subirPortada(id: number, archivo: File): Promise<EventoDTO> {
  const datos = new FormData();
  datos.append('archivo', archivo);
  return httpClient.putArchivo<EventoDTO>(`/api/eventos/${id}/portada`, datos);
}

export function quitarPortada(id: number): Promise<EventoDTO> {
  return httpClient.delete<EventoDTO>(`/api/eventos/${id}/portada`);
}

/**
 * La portada bajada con el token. Hace falta para la vista previa de un borrador: su portada no es
 * pública y un <img> no manda el header Authorization.
 */
export function bajarPortada(portadaUrl: string): Promise<Blob> {
  return httpClient.blob(portadaUrl);
}
