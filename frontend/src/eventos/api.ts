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
