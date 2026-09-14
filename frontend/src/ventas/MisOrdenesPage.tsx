import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { EventoDTO } from '../eventos/types';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { useAuth } from '../lib/auth/AuthContext';
import { listarOrdenesDeComprador } from './api';
import { diaYMes, formatearFechaEvento, formatearPrecio, pluralEntradas } from './formato';
import type { ItemDeOrdenDTO, OrdenDTO } from './types';
import { nombreDeEvento, useEventos } from './useEventos';

type Pestania = 'proximas' | 'pasadas';

// Una orden puede mezclar entradas de varios eventos; el diseño (Figma Web 06) muestra una
// tarjeta por evento, así que cada orden se abre en un grupo por evento.
interface GrupoDeOrden {
  orden: OrdenDTO;
  idEvento: number;
  items: ItemDeOrdenDTO[];
}

function agrupar(ordenes: OrdenDTO[]): GrupoDeOrden[] {
  return ordenes.flatMap((orden) => {
    const porEvento = new Map<number, ItemDeOrdenDTO[]>();
    for (const item of orden.items) {
      porEvento.set(item.idEvento, [...(porEvento.get(item.idEvento) ?? []), item]);
    }
    return [...porEvento].map(([idEvento, items]) => ({ orden, idEvento, items }));
  });
}

function esPasado(evento: EventoDTO | null | undefined): boolean {
  return evento ? new Date(evento.fechaHora).getTime() < Date.now() : false;
}

export function MisOrdenesPage() {
  const { usuario } = useAuth();
  const [ordenes, setOrdenes] = useState<OrdenDTO[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [pestania, setPestania] = useState<Pestania>('proximas');
  const eventos = useEventos(ordenes.flatMap((orden) => orden.items.map((item) => item.idEvento)));

  useEffect(() => {
    if (!usuario) return;
    let vigente = true;

    listarOrdenesDeComprador(usuario.id)
      .then((resultado) => {
        if (vigente) setOrdenes(resultado);
      })
      .catch((causa: unknown) => {
        if (vigente) setError(causa);
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });

    return () => {
      vigente = false;
    };
  }, [usuario]);

  const grupos = agrupar(ordenes)
    .filter((grupo) => esPasado(eventos.get(grupo.idEvento)) === (pestania === 'pasadas'))
    .sort((a, b) => new Date(b.orden.creadaEn).getTime() - new Date(a.orden.creadaEn).getTime());

  return (
    <section className="stack-grande">
      <div className="encabezado-pagina">
        <div>
          <p className="eyebrow">Ventas</p>
          <h2>Mis órdenes</h2>
        </div>
      </div>

      <div className="tabs" role="tablist" aria-label="Filtrar órdenes">
        {(['proximas', 'pasadas'] as const).map((valor) => (
          <button
            key={valor}
            className={pestania === valor ? 'tab tab-activa' : 'tab'}
            type="button"
            role="tab"
            aria-selected={pestania === valor}
            onClick={() => setPestania(valor)}
          >
            {valor === 'proximas' ? 'Próximas' : 'Pasadas'}
          </button>
        ))}
      </div>

      {error ? <EstadoError error={error} /> : null}

      {cargando ? (
        <EstadoCarga mensaje="Cargando tus órdenes..." />
      ) : grupos.length === 0 && !error ? (
        <div className="panel estado-vacio stack-chico">
          <h3>{pestania === 'proximas' ? 'No tenés entradas para próximos eventos' : 'No tenés órdenes de eventos pasados'}</h3>
          {pestania === 'proximas' ? (
            <div>
              <Link className="boton boton-primario" to="/">Ver la cartelera</Link>
            </div>
          ) : null}
        </div>
      ) : (
        <div className="stack-chico">
          {grupos.map(({ orden, idEvento, items }) => {
            const evento = eventos.get(idEvento);
            const cantidad = items.reduce((suma, item) => suma + item.cantidad, 0);
            const subtotal = items.reduce((suma, item) => suma + item.precioUnitario * item.cantidad, 0);
            const fecha = evento ? diaYMes(evento.fechaHora) : null;
            return (
              <article className="tarjeta-orden" key={`${orden.id}-${idEvento}`}>
                <div className="flyer-fecha" aria-hidden="true">
                  {fecha ? (
                    <>
                      <span className="flyer-dia">{fecha.dia}</span>
                      <span className="flyer-mes">{fecha.mes}</span>
                    </>
                  ) : null}
                </div>
                <div className="tarjeta-orden-info">
                  <h3>{nombreDeEvento(eventos, idEvento)}</h3>
                  {evento ? (
                    <p>{formatearFechaEvento(evento.fechaHora)} · {evento.lugar}</p>
                  ) : null}
                  <p>
                    {pluralEntradas(cantidad)} · {items.map((item) => item.nombreTipoEntrada).join(', ')}
                  </p>
                  <p>Orden #{orden.id}</p>
                </div>
                <span className="badge badge-pagada">● Pagada</span>
                <span className="tarjeta-orden-total">{formatearPrecio(subtotal)}</span>
                <Link className="boton boton-primario" to={`/mis-ordenes/${orden.id}`}>
                  Ver detalle
                </Link>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
