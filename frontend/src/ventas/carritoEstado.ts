import { useEffect, useState } from 'react';
import type { CarritoDTO } from './types';
const CLAVE = 'passly.carrito.cantidad';
export function actualizarCarrito(carrito: CarritoDTO | null) {
  const cantidad =
    carrito && !carrito.expirado
      ? carrito.items.reduce((n, i) => n + i.cantidad, 0)
      : 0;
  sessionStorage.setItem(CLAVE, String(cantidad));
  window.dispatchEvent(new Event('passly:carrito'));
  return carrito;
}
export function registrarCarrito(carrito: CarritoDTO) {
  actualizarCarrito(carrito);
  return carrito;
}
export function useCantidadCarrito() {
  const leer = () => Number(sessionStorage.getItem(CLAVE) || 0);
  const [cantidad, setCantidad] = useState(leer);
  useEffect(() => {
    const recibir = () => setCantidad(leer());
    window.addEventListener('passly:carrito', recibir);
    return () => window.removeEventListener('passly:carrito', recibir);
  }, []);
  return cantidad;
}
