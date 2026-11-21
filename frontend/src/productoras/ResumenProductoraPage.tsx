import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { listarEventosDeProductora } from '../eventos/api';
import type { EventoDTO } from '../eventos/types';
import { EstadoCarga } from '../layout/EstadoCarga';
import { EstadoError } from '../layout/EstadoError';
import { formatearFechaEvento } from '../ventas/formato';
import { useProductoraActual } from './GestionLayout';
import { calcularMetricas, proximosEventos, type VentasDeEvento } from './metricas';

const NUMERO = new Intl.NumberFormat('es-AR');

/**
 * Resumen del dashboard (Figma Web 20). Cada bloque es una .dashboard-tarjeta dentro de una
 * grilla: un gráfico nuevo (ventas por día, ingresos, validaciones) es una tarjeta más.
 */
export function ResumenProductoraPage() {
  const p = useProductoraActual();
  const location = useLocation();
  const [eventos, setEventos] = useState<EventoDTO[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    if (!p.puedeGestionar) return;
    let vigente = true;
    listarEventosDeProductora(p.id)
      .then((v) => {
        if (vigente) setEventos(v);
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, [p.id, p.puedeGestionar, intento]);

  function reintentar() {
    setError(null);
    setIntento((n) => n + 1);
  }

  const nueva = (location.state as { productoraCreada?: boolean } | null)?.productoraCreada;
  const base = '/productoras/' + p.id;

  return (
    <section className="stack-grande">
      {nueva ? (
        <p className="mensaje-exito" role="status">
          Tu productora está lista. Ya podés preparar tu primer evento.
        </p>
      ) : null}
      <header className="dashboard-encabezado">
        <div>
          <h1 className="dashboard-titulo">Resumen</h1>
          <p className="texto-secundario">
            {p.nombreComercial}
            {p.puedeGestionar ? ' · cómo vienen tus eventos' : ''}
          </p>
        </div>
        {p.puedeGestionar ? (
          <Link className="boton boton-primario" to={base + '/eventos/nuevo'}>
            + Crear evento
          </Link>
        ) : null}
      </header>

      {!p.puedeGestionar ? (
        <div className="dashboard-tarjeta stack-chico">
          <h3>Controlá el ingreso a tus eventos</h3>
          <p>Elegí un evento publicado de esta productora para empezar.</p>
          <div>
            <Link className="boton boton-primario" to={'/validar?productora=' + p.id}>
              Validar entradas
            </Link>
          </div>
        </div>
      ) : error ? (
        <EstadoError error={error} onReintentar={reintentar} />
      ) : !eventos ? (
        <EstadoCarga mensaje="Cargando tus eventos…" />
      ) : eventos.length === 0 ? (
        <div className="dashboard-tarjeta stack-chico">
          <h3>Prepará tu primer evento</h3>
          <p>
            Guardá un borrador, cargá sus entradas y revisá todo antes de publicarlo. Cuando
            vendas, acá vas a ver cómo viene cada evento.
          </p>
          <div>
            <Link className="boton boton-primario" to={base + '/eventos/nuevo'}>
              Crear mi primer evento
            </Link>
          </div>
        </div>
      ) : (
        <Tablero eventos={eventos} base={base} />
      )}

      {p.puedeInvitar ? (
        <div className="dashboard-tarjeta stack-chico">
          <h3>Sumá a tu equipo</h3>
          <p>Invitá staff para gestionar eventos o validadores para controlar el ingreso.</p>
          <div>
            <Link className="boton boton-secundario" to={base + '/backoffice'}>
              Invitar a mi equipo
            </Link>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function Tablero({ eventos, base }: { eventos: EventoDTO[]; base: string }) {
  const m = calcularMetricas(eventos);
  const proximos = proximosEventos(eventos).slice(0, 4);

  return (
    <>
      <dl className="dashboard-metricas">
        <Metrica
          titulo="Eventos"
          valor={NUMERO.format(m.eventos)}
          detalle={m.borradores === 1 ? '1 borrador' : `${m.borradores} borradores`}
        />
        <Metrica
          titulo="Publicados"
          valor={NUMERO.format(m.publicados)}
          detalle={m.publicados ? 'a la venta o ya realizados' : 'ninguno a la venta'}
        />
        <Metrica
          titulo="Entradas vendidas"
          valor={NUMERO.format(m.vendidas)}
          detalle={`de ${NUMERO.format(m.cupoPublicado)} de cupo`}
        />
        <Metrica titulo="Ocupación" valor={`${m.ocupacion}%`} detalle="de los eventos publicados" />
      </dl>

      <div className="dashboard-grilla">
        <article className="dashboard-tarjeta dashboard-tarjeta-ancha">
          <header>
            <h3>Entradas vendidas por evento</h3>
            <p className="texto-terciario">Vendidas sobre el cupo total de cada evento publicado</p>
          </header>
          {m.ventasPorEvento.length ? (
            <ul className="grafico-barras">
              {m.ventasPorEvento.map((v) => (
                <BarraDeVentas key={v.id} ventas={v} base={base} />
              ))}
            </ul>
          ) : (
            <p className="texto-secundario">
              Cuando publiques un evento vas a ver acá cómo se venden sus entradas.
            </p>
          )}
        </article>

        <article className="dashboard-tarjeta">
          <header>
            <h3>Próximos eventos</h3>
          </header>
          {proximos.length ? (
            <ul className="lista-proximos">
              {proximos.map((e) => (
                <li key={e.id}>
                  <Link to={base + '/eventos/' + e.id}>
                    <strong>{e.nombre}</strong>
                    <span className="texto-terciario">{formatearFechaEvento(e.fechaHora)}</span>
                  </Link>
                  <span className={'badge badge-' + e.estado.toLowerCase()}>
                    {e.estado === 'PUBLICADO' ? 'Publicado' : 'Borrador'}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="texto-secundario">No tenés eventos por delante.</p>
          )}
          <Link className="enlace-tarjeta" to={base + '/eventos'}>
            Ver todos los eventos →
          </Link>
        </article>
      </div>
    </>
  );
}

function Metrica({ titulo, valor, detalle }: { titulo: string; valor: string; detalle: string }) {
  return (
    <div className="dashboard-metrica">
      <dt>{titulo}</dt>
      <dd>{valor}</dd>
      <dd className="dashboard-metrica-detalle">{detalle}</dd>
    </div>
  );
}

function BarraDeVentas({ ventas, base }: { ventas: VentasDeEvento; base: string }) {
  return (
    <li>
      <div className="grafico-barras-etiqueta">
        <Link to={base + '/eventos/' + ventas.id}>{ventas.nombre}</Link>
        <span className="texto-secundario">
          {NUMERO.format(ventas.vendidas)} / {NUMERO.format(ventas.cupo)}
        </span>
      </div>
      <div
        className="grafico-barras-pista"
        role="progressbar"
        aria-label={`${ventas.nombre}: ${ventas.ocupacion}% vendido`}
        aria-valuenow={ventas.ocupacion}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div className="grafico-barras-relleno" style={{ width: `${ventas.ocupacion}%` }} />
      </div>
    </li>
  );
}
