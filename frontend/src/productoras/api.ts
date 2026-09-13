import { httpClient } from '../lib/http/httpClient';
import type {
  AgregarMiembroRequest,
  CrearProductoraRequest,
  MiembroDTO,
  ProductoraDTO,
} from './types';

export function listarProductoras(): Promise<ProductoraDTO[]> {
  return httpClient.get<ProductoraDTO[]>('/api/productoras');
}

export function misProductoras(): Promise<ProductoraDTO[]> {
  return httpClient.get<ProductoraDTO[]>('/api/productoras/mias');
}

export function obtenerProductora(id: number): Promise<ProductoraDTO> {
  return httpClient.get<ProductoraDTO>(`/api/productoras/${id}`);
}

export function crearProductora(request: CrearProductoraRequest): Promise<ProductoraDTO> {
  return httpClient.post<ProductoraDTO>('/api/productoras', request);
}

export function listarMiembros(idProductora: number): Promise<MiembroDTO[]> {
  return httpClient.get<MiembroDTO[]>(`/api/productoras/${idProductora}/miembros`);
}

export function agregarMiembro(
  idProductora: number,
  request: AgregarMiembroRequest,
): Promise<MiembroDTO> {
  return httpClient.post<MiembroDTO>(`/api/productoras/${idProductora}/miembros`, request);
}
