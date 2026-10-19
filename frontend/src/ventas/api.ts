import { httpClient } from '../lib/http/httpClient';
import { registrarCarrito, actualizarCarrito } from './carritoEstado';
import type {
  AgregarItemRequest,
  CarritoDTO,
  DatosDeTarjetaDTO,
  OrdenDTO,
  TicketDTO,
} from './types';

export function obtenerCarrito(): Promise<CarritoDTO> {
  return httpClient
    .get<CarritoDTO>('/api/ventas/carrito')
    .then(registrarCarrito);
}

export function agregarItemAlCarrito(
  request: AgregarItemRequest,
): Promise<CarritoDTO> {
  return httpClient
    .post<CarritoDTO>('/api/ventas/carrito/items', request)
    .then(registrarCarrito);
}

// El backend devuelve la foto del carrito antes de invalidar la sesión.
export function vaciarCarrito(): Promise<CarritoDTO> {
  return httpClient.delete<CarritoDTO>('/api/ventas/carrito').then((v) => {
    actualizarCarrito(null);
    return v;
  });
}

export function confirmarCompra(tarjeta: DatosDeTarjetaDTO): Promise<OrdenDTO> {
  return httpClient.post<OrdenDTO>('/api/ventas/ordenes', { tarjeta });
}

export function obtenerOrden(id: number): Promise<OrdenDTO> {
  return httpClient.get<OrdenDTO>(`/api/ventas/ordenes/${id}`);
}

export function obtenerTicketsDeOrden(idOrden: number): Promise<TicketDTO[]> {
  return httpClient.get<TicketDTO[]>(`/api/tickets/ordenes/${idOrden}`);
}

export function listarOrdenesDeComprador(
  idComprador: number,
): Promise<OrdenDTO[]> {
  return httpClient.get<OrdenDTO[]>(
    `/api/ventas/compradores/${idComprador}/ordenes`,
  );
}
