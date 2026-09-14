import { useEffect, useState } from 'react';
import type { CarritoDTO } from './types';

/**
 * Cuenta regresiva del carrito (~5 min desde que el contenedor lo creó, CLAUDE.md §4.7).
 *
 * Parte de `segundosRestantes` —calculado por el servidor con su propio reloj— y del instante en
 * que llegó la respuesta, no de `expiraEn`: comparar `expiraEn` contra el reloj del navegador
 * haría que un cliente con la hora corrida viera el carrito vencido antes o después de tiempo.
 * Es solo la vista; quien decide si el carrito venció sigue siendo el backend al confirmar.
 */
export function useCuentaRegresiva(carrito: CarritoDTO | null, recibidoEn: number) {
  const [ahora, setAhora] = useState(() => Date.now());

  const segundosIniciales = carrito ? (carrito.expirado ? 0 : carrito.segundosRestantes) : 0;
  // `ahora` puede ser anterior a `recibidoEn` justo después de re-pedir el carrito.
  const transcurridos = Math.floor(Math.max(0, ahora - recibidoEn) / 1000);
  const restantes = Math.max(0, segundosIniciales - transcurridos);
  const activa = carrito !== null && restantes > 0;

  useEffect(() => {
    if (!activa) return;
    const intervalo = window.setInterval(() => setAhora(Date.now()), 1000);
    return () => window.clearInterval(intervalo);
  }, [activa]);

  const minutos = String(Math.floor(restantes / 60)).padStart(2, '0');
  const segundos = String(restantes % 60).padStart(2, '0');

  return {
    texto: `${minutos}:${segundos}`,
    vencido: carrito !== null && restantes === 0,
  };
}
