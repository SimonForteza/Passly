import { httpClient } from '../lib/http/httpClient';
import type { AgregarItemRequest, CarritoDTO, OrdenDTO } from './types';

export function obtenerCarrito(): Promise<CarritoDTO> {
  return httpClient.get<CarritoDTO>('/api/ventas/carrito');
}

export function agregarItemAlCarrito(request: AgregarItemRequest): Promise<CarritoDTO> {
  return httpClient.post<CarritoDTO>('/api/ventas/carrito/items', request);
}

export function vaciarCarrito(): Promise<void> {
  return httpClient.delete<void>('/api/ventas/carrito');
}

export function confirmarCompra(): Promise<OrdenDTO> {
  return httpClient.post<OrdenDTO>('/api/ventas/ordenes');
}

export function obtenerOrden(id: number): Promise<OrdenDTO> {
  return httpClient.get<OrdenDTO>(`/api/ventas/ordenes/${id}`);
}

export function listarOrdenesDeComprador(idComprador: number): Promise<OrdenDTO[]> {
  return httpClient.get<OrdenDTO[]>(`/api/ventas/compradores/${idComprador}/ordenes`);
}
