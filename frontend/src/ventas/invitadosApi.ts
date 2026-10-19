import { httpClient } from '../lib/http/httpClient';
import { registrarCarrito, actualizarCarrito } from './carritoEstado';
import type {
  CarritoDTO,
  DatosDeTarjetaDTO,
  OrdenDTO,
  TicketDTO,
  AgregarItemRequest,
} from './types';
export interface DetalleInvitado {
  orden: OrdenDTO;
  tickets: TicketDTO[];
}
export interface CompraInvitada {
  detalle: DetalleInvitado;
  token: string;
}
const base = '/api/ventas/invitados';
export const verCarritoInvitado = () =>
  httpClient.get<CarritoDTO>(`${base}/carrito`).then(registrarCarrito);
export async function agregarInvitado(items: AgregarItemRequest[]) {
  const actual = await verCarritoInvitado();
  if (actual.expirado) await vaciarInvitado();
  for (const item of items)
    registrarCarrito(
      await httpClient.post<CarritoDTO>(`${base}/carrito/items`, item),
    );
}
export const transferirCarritoInvitado = () =>
  httpClient.post<CarritoDTO>(`${base}/transferencia`).then(registrarCarrito);
export const vaciarInvitado = () =>
  httpClient.delete<void>(`${base}/carrito`).then(() => {
    actualizarCarrito(null);
  });
export const comprarInvitado = (
  nombre: string,
  email: string,
  clave: string,
  tarjeta: DatosDeTarjetaDTO,
) =>
  httpClient.post<CompraInvitada>(
    `${base}/ordenes`,
    { nombre, email, tarjeta },
    { headers: { 'Idempotency-Key': clave } },
  );
export const consultarInvitado = (token: string) =>
  httpClient.get<DetalleInvitado>(`${base}/orden`, {
    headers: { 'X-Orden-Token': token },
  });
export const recuperarInvitado = (orden: number, email: string) =>
  httpClient.post<void>(`${base}/recuperacion`, { orden, email });
