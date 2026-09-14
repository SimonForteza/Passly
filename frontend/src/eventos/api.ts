import { httpClient } from '../lib/http/httpClient';
import type { CrearEventoRequest, DisponibilidadDTO, EventoDTO } from './types';

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

export function listarEventosDeProductora(idProductora: number): Promise<EventoDTO[]> {
  return httpClient.get<EventoDTO[]>(`/api/productoras/${idProductora}/eventos`);
}

export function consultarDisponibilidad(idTipoEntrada: number): Promise<DisponibilidadDTO> {
  return httpClient.get<DisponibilidadDTO>(`/api/tipos-entrada/${idTipoEntrada}/disponibilidad`);
}
