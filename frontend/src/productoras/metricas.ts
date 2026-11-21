import type { EventoDTO } from '../eventos/types';

export interface VentasDeEvento {
  id: number;
  nombre: string;
  vendidas: number;
  cupo: number;
  /** 0 a 100. 0 si el evento no tiene cupo cargado. */
  ocupacion: number;
}

export interface MetricasDeProductora {
  eventos: number;
  borradores: number;
  publicados: number;
  vendidas: number;
  cupoPublicado: number;
  /** Vendidas sobre el cupo de los eventos publicados, 0 a 100. */
  ocupacion: number;
  /** Publicados, de mayor a menor cantidad vendida. */
  ventasPorEvento: VentasDeEvento[];
}

/** Lo vendido sale del cupo: la API no expone ventas, pero el cupo disponible ya las descuenta. */
export function ventasDeEvento(evento: EventoDTO): VentasDeEvento {
  const cupo = evento.tiposEntrada.reduce((total, t) => total + t.cupoTotal, 0);
  const vendidas = evento.tiposEntrada.reduce(
    (total, t) => total + (t.cupoTotal - t.cupoDisponible),
    0,
  );
  return {
    id: evento.id,
    nombre: evento.nombre,
    vendidas,
    cupo,
    ocupacion: porcentaje(vendidas, cupo),
  };
}

export function calcularMetricas(eventos: EventoDTO[]): MetricasDeProductora {
  const publicados = eventos.filter((e) => e.estado === 'PUBLICADO');
  const ventasPorEvento = publicados
    .map(ventasDeEvento)
    .sort((a, b) => b.vendidas - a.vendidas);
  const vendidas = ventasPorEvento.reduce((t, v) => t + v.vendidas, 0);
  const cupoPublicado = ventasPorEvento.reduce((t, v) => t + v.cupo, 0);
  return {
    eventos: eventos.length,
    borradores: eventos.filter((e) => e.estado === 'BORRADOR').length,
    publicados: publicados.length,
    vendidas,
    cupoPublicado,
    ocupacion: porcentaje(vendidas, cupoPublicado),
    ventasPorEvento,
  };
}

/** Eventos que todavía no pasaron y no están cancelados, del más cercano al más lejano. */
export function proximosEventos(eventos: EventoDTO[], ahora = new Date()): EventoDTO[] {
  return eventos
    .filter((e) => e.estado !== 'CANCELADO' && new Date(e.fechaHora) >= ahora)
    .sort((a, b) => a.fechaHora.localeCompare(b.fechaHora));
}

function porcentaje(parte: number, total: number): number {
  return total > 0 ? Math.round((parte / total) * 100) : 0;
}
