import { httpClient } from '../lib/http/httpClient';
import type { AgregarItemRequest, CarritoDTO, OrdenDTO, TicketDTO } from './types';

export function obtenerCarrito(): Promise<CarritoDTO> {
  return httpClient.get<CarritoDTO>('/api/ventas/carrito');
}

export function agregarItemAlCarrito(request: AgregarItemRequest): Promise<CarritoDTO> {
  return httpClient.post<CarritoDTO>('/api/ventas/carrito/items', request);
}

// 200 con la foto del carrito tomada antes de invalidar la sesion (VentaController.abandonarCarrito).
export function vaciarCarrito(): Promise<CarritoDTO> {
  return httpClient.delete<CarritoDTO>('/api/ventas/carrito');
}

export function confirmarCompra(): Promise<OrdenDTO> {
  return httpClient.post<OrdenDTO>('/api/ventas/ordenes');
}

export function obtenerOrden(id: number): Promise<OrdenDTO> {
  return httpClient.get<OrdenDTO>(`/api/ventas/ordenes/${id}`);
}

export function obtenerTicketsDeOrden(idOrden: number): Promise<TicketDTO[]> {
  return httpClient.get<TicketDTO[]>(`/api/tickets/ordenes/${idOrden}`);
}

export function listarOrdenesDeComprador(idComprador: number): Promise<OrdenDTO[]> {
  return httpClient.get<OrdenDTO[]>(`/api/ventas/compradores/${idComprador}/ordenes`);
}
