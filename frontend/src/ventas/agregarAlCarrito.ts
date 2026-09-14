import { agregarItemAlCarrito, obtenerCarrito, vaciarCarrito } from './api';
import type { AgregarItemRequest, CarritoDTO } from './types';

export interface ResultadoDeAgregar {
  carrito: CarritoDTO;
  // true si había un carrito vencido y se descartó antes de agregar.
  seReinicio: boolean;
}

/**
 * Suma las líneas elegidas al carrito de la sesión.
 *
 * El reloj del carrito (`expiraEn`) se fija cuando el contenedor crea el bean y no vuelve a
 * empezar aunque el carrito se vacíe: un carrito vencido acepta ítems nuevos pero nunca se va a
 * poder confirmar. Por eso, si el actual ya venció, primero se abandona (DELETE invalida la sesión
 * y el próximo request nace con un carrito y un reloj nuevos) y recién después se agrega.
 *
 * Las líneas se mandan de a una: el backend funde las del mismo tipo de entrada, y cada request
 * autenticado rota el JSESSIONID (ver la cola de httpClient).
 */
export async function agregarAlCarrito(lineas: AgregarItemRequest[]): Promise<ResultadoDeAgregar> {
  const actual = await obtenerCarrito();
  const seReinicio = actual.expirado;
  if (seReinicio) {
    await vaciarCarrito();
  }

  let carrito = actual;
  for (const linea of lineas) {
    carrito = await agregarItemAlCarrito(linea);
  }
  return { carrito, seReinicio };
}
