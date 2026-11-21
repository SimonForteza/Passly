import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { listarEventos } from './api';
import { enPeriodo, PERIODOS, type Periodo } from './periodos';
import { TarjetaEvento } from './TarjetaEvento';
import type { EventoDTO } from './types';

// Minúsculas y sin tildes, para que "recital" encuentre "Recital" y "musica" encuentre "Música".
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

export function CarteleraPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  // El filtro por productora sigue viviendo en la URL (/?productora=3, link compartible de PAS-26),
  // aunque desde PAS-47 la pantalla ya no tiene selector: solo se muestra para poder quitarlo.
  // La búsqueda y el período son estado local y filtran en el cliente sobre lo ya cargado.
  const filtroProductora = searchParams.get('productora') ?? '';
  const [busqueda, setBusqueda] = useState('');
  const [periodo, setPeriodo] = useState<Periodo>('todos');
  const [eventos, setEventos] = useState<EventoDTO[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    setCargando(true);
    setError(null);

    const idProductora = filtroProductora
      ? Number(filtroProductora)
      : undefined;

    listarEventos(idProductora)
      .then((lista) => {
        if (vigente) setEventos(lista);
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
  }, [filtroProductora, intento]);

  function reintentar() {
    setError(null);
    setCargando(true);
    setIntento((n) => n + 1);
  }

  const termino = normalizar(busqueda.trim());
  const visibles = eventos.filter(
    (evento) =>
      enPeriodo(evento.fechaHora, periodo) &&
      (!termino || normalizar(`${evento.nombre} ${evento.lugar}`).includes(termino)),
  );
  const nombreProductora = filtroProductora
    ? eventos[0]?.organizador.nombreComercial ?? 'la productora'
    : null;
  const nombrePeriodo = PERIODOS.find((p) => p.id === periodo)!.nombre.toLowerCase();

  return (
    <section className="stack-grande">
      <h2 className="titulo-cartelera">Próximos eventos</h2>

      {/* Encabezado de Figma Web 25: buscador en píldora y chips por fecha. */}
      <div className="filtros-cartelera">
        <label className="buscador-pildora">
          <span className="sr-only">Buscar evento</span>
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            type="search"
            value={busqueda}
            onChange={(evento) => setBusqueda(evento.target.value)}
            placeholder="Buscar evento…"
          />
        </label>
        <div className="chips" role="group" aria-label="Filtrar por fecha">
          {PERIODOS.map((p) => (
            <button
              key={p.id}
              type="button"
              className={'chip' + (periodo === p.id ? ' chip-activo' : '')}
              aria-pressed={periodo === p.id}
              onClick={() => setPeriodo(p.id)}
            >
              {p.nombre}
            </button>
          ))}
          {nombreProductora ? (
            <button
              type="button"
              className="chip chip-activo"
              aria-label={`Quitar el filtro de ${nombreProductora}`}
              onClick={() => setSearchParams({})}
            >
              {nombreProductora} ✕
            </button>
          ) : null}
        </div>
      </div>

      {error ? <EstadoError error={error} onReintentar={reintentar} /> : null}

      {cargando ? (
        <EstadoCarga mensaje="Cargando la cartelera..." />
      ) : eventos.length === 0 ? (
        <div className="panel estado-vacio">
          <h3>No hay eventos publicados</h3>
          <p className="texto-secundario">
            {filtroProductora
              ? 'Esta productora no tiene eventos publicados por ahora.'
              : 'Volvé a mirar más tarde.'}
          </p>
        </div>
      ) : visibles.length === 0 ? (
        <div className="panel estado-vacio">
          <h3>
            {termino
              ? `Ningún evento coincide con “${busqueda.trim()}”`
              : `No hay eventos ${nombrePeriodo}`}
          </h3>
          <p className="texto-secundario">
            {termino ? 'Probá con otro nombre o lugar.' : 'Probá con otro período.'}
          </p>
        </div>
      ) : (
        <div className="grilla-portadas">
          {visibles.map((evento) => (
            <TarjetaEvento key={evento.id} evento={evento} />
          ))}
        </div>
      )}
    </section>
  );
}
