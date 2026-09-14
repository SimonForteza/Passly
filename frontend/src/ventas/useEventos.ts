import { useEffect, useState } from 'react';
import { obtenerEvento } from '../eventos/api';
import type { EventoDTO } from '../eventos/types';

// Las líneas del carrito y de las órdenes traen `idEvento` pero no nombre, fecha ni lugar: Ventas
// no copia datos de Eventos en su contrato. La pantalla los resuelve contra el endpoint público
// de Eventos, y los guarda acá para no repetir el pedido al pasar de una pantalla a otra.
const cache = new Map<number, EventoDTO | null>();

export function useEventos(idsEventos: number[]) {
  const clave = [...new Set(idsEventos)].sort((a, b) => a - b).join(',');
  const [eventos, setEventos] = useState<Map<number, EventoDTO | null>>(() => new Map(cache));

  useEffect(() => {
    if (!clave) return;
    let vigente = true;
    const faltantes = clave.split(',').map(Number).filter((id) => !cache.has(id));
    if (faltantes.length === 0) return;

    Promise.all(
      faltantes.map((id) =>
        obtenerEvento(id)
          .then((evento) => cache.set(id, evento))
          // Si el evento no se puede leer, la pantalla muestra "Evento #id" en vez de romperse.
          .catch(() => cache.set(id, null)),
      ),
    ).then(() => {
      if (vigente) setEventos(new Map(cache));
    });

    return () => {
      vigente = false;
    };
  }, [clave]);

  return eventos;
}

export function nombreDeEvento(eventos: Map<number, EventoDTO | null>, idEvento: number): string {
  return eventos.get(idEvento)?.nombre ?? `Evento #${idEvento}`;
}
