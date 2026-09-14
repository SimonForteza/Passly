import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { listarProductoras } from '../productoras/api';
import type { ProductoraDTO } from '../productoras/types';
import { listarEventos } from './api';
import type { EventoDTO } from './types';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', {
  weekday: 'short',
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

function precioDesde(evento: EventoDTO): number | null {
  if (evento.tiposEntrada.length === 0) return null;
  return Math.min(...evento.tiposEntrada.map((tipo) => tipo.precio));
}

export function CarteleraPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filtroProductora = searchParams.get('productora') ?? '';
  const [eventos, setEventos] = useState<EventoDTO[]>([]);
  const [productoras, setProductoras] = useState<ProductoraDTO[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let vigente = true;
    setCargando(true);
    setError(null);

    const idProductora = filtroProductora ? Number(filtroProductora) : undefined;

    Promise.all([listarEventos(idProductora), listarProductoras()])
      .then(([listaEventos, listaProductoras]) => {
        if (!vigente) return;
        setEventos(listaEventos);
        setProductoras(listaProductoras);
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
  }, [filtroProductora]);

  function cambiarFiltro(idProductora: string) {
    if (idProductora) {
      setSearchParams({ productora: idProductora });
    } else {
      setSearchParams({});
    }
  }

  return (
    <section className="stack-grande">
      <div className="encabezado-pagina">
        <div>
          <p className="eyebrow">Cartelera</p>
          <h2>Próximos eventos</h2>
          <p className="texto-secundario">Entradas con QR único para la noche.</p>
        </div>
        <label>
          Productora
          <select value={filtroProductora} onChange={(evento) => cambiarFiltro(evento.target.value)}>
            <option value="">Todas</option>
            {productoras.map((productora) => (
              <option key={productora.id} value={productora.id}>
                {productora.nombreComercial}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error ? <EstadoError error={error} /> : null}

      {cargando ? (
        <EstadoCarga mensaje="Cargando la cartelera..." />
      ) : eventos.length === 0 ? (
        <div className="panel estado-vacio">
          <h3>No hay eventos publicados</h3>
          <p className="texto-secundario">
            {filtroProductora ? 'Esta productora no tiene eventos publicados por ahora.' : 'Volvé a mirar más tarde.'}
          </p>
        </div>
      ) : (
        <div className="grilla-tarjetas">
          {eventos.map((evento) => {
            const desde = precioDesde(evento);
            return (
              <article className="panel tarjeta-evento" key={evento.id}>
                <div>
                  <p className="eyebrow">{evento.organizador.nombreComercial}</p>
                  <h3>{evento.nombre}</h3>
                  <p className="texto-secundario">
                    {FORMATO_FECHA.format(new Date(evento.fechaHora))} · {evento.lugar}
                  </p>
                </div>
                <div className="tarjeta-evento-pie">
                  <span className="texto-secundario">
                    {desde !== null ? `Desde $${desde.toLocaleString('es-AR')}` : 'Sin entradas cargadas'}
                  </span>
                  <Link className="boton boton-primario" to={`/eventos/${evento.id}`}>
                    Ver entradas
                  </Link>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
